/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com)
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.ballerina.flowmodelgenerator.extension;

import io.ballerina.modelgenerator.commons.SearchDatabaseManager;
import io.ballerina.modelgenerator.commons.SearchResult;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Regression tests for {@link SearchDatabaseManager}'s org-aware matching in {@code searchTypesByPackages} and
 * {@code findIndexedModuleNames}. The shipped {@code search-index.sqlite} legitimately indexes the same package
 * name under more than one org (e.g. {@code copybook} under both {@code ballerina} and {@code ballerinax}, since
 * {@code Package.name} has no uniqueness constraint) - these tests pin down that a query for one org's module
 * never returns or counts another org's rows for the same name.
 *
 * @since 1.0.0
 */
public class SearchDatabaseManagerOrgMatchTest {

    private final SearchDatabaseManager dbManager = SearchDatabaseManager.getInstance();

    @Test(description = "searchTypesByPackages must only return the requested org's types for a colliding name")
    public void testSearchTypesByPackagesMatchesRequestedOrgOnly() {
        // ballerinax/copybook has 3 indexed types, ballerina/copybook (same name, different org) has 0.
        List<SearchResult> ballerinaxResults =
                dbManager.searchTypesByPackages(Map.of("copybook", "ballerinax"), 100, 0);
        Assert.assertEquals(ballerinaxResults.size(), 3);
        for (SearchResult result : ballerinaxResults) {
            Assert.assertEquals(result.packageInfo().org(), "ballerinax");
        }

        List<SearchResult> ballerinaResults =
                dbManager.searchTypesByPackages(Map.of("copybook", "ballerina"), 100, 0);
        Assert.assertTrue(ballerinaResults.isEmpty(),
                "ballerina/copybook has no types of its own and must not fall back to ballerinax/copybook's rows");
    }

    @Test(description = "searchTypesByPackages must not leak the other org's rows for the reverse collision")
    public void testSearchTypesByPackagesDoesNotLeakAcrossOrgs() {
        // ballerina/np has 3 indexed types, ballerinax/np (same name, different org) has 0.
        List<SearchResult> ballerinaxResults = dbManager.searchTypesByPackages(Map.of("np", "ballerinax"), 100, 0);
        Assert.assertTrue(ballerinaxResults.isEmpty(),
                "ballerinax/np has no types of its own and must not fall back to ballerina/np's rows");

        List<SearchResult> ballerinaResults = dbManager.searchTypesByPackages(Map.of("np", "ballerina"), 100, 0);
        Assert.assertEquals(ballerinaResults.size(), 3);
        for (SearchResult result : ballerinaResults) {
            Assert.assertEquals(result.packageInfo().org(), "ballerina");
        }
    }

    @Test(description = "searchTypesByPackages must not merge two orgs' types for the same package name")
    public void testSearchTypesByPackagesDoesNotMergeCollidingOrgs() {
        // xlibb/solace has 32 indexed types, ballerinax/solace (same name, different org) has 29.
        List<SearchResult> xlibbResults = dbManager.searchTypesByPackages(Map.of("solace", "xlibb"), 100, 0);
        Assert.assertEquals(xlibbResults.size(), 32);
        for (SearchResult result : xlibbResults) {
            Assert.assertEquals(result.packageInfo().org(), "xlibb");
        }

        List<SearchResult> ballerinaxResults =
                dbManager.searchTypesByPackages(Map.of("solace", "ballerinax"), 100, 0);
        Assert.assertEquals(ballerinaxResults.size(), 29);
        for (SearchResult result : ballerinaxResults) {
            Assert.assertEquals(result.packageInfo().org(), "ballerinax");
        }
    }

    @Test(description = "findIndexedModuleNames must not report a module as indexed under the wrong org")
    public void testFindIndexedModuleNamesMatchesOrg() {
        Set<String> indexedUnderRealOrg = dbManager.findIndexedModuleNames(Map.of("copybook", "ballerina"));
        Assert.assertTrue(indexedUnderRealOrg.contains("copybook"),
                "ballerina/copybook is indexed (with zero types) and must still count as indexed");

        Set<String> indexedUnderFakeOrg =
                dbManager.findIndexedModuleNames(Map.of("copybook", "totally-fake-org-xyz"));
        Assert.assertTrue(indexedUnderFakeOrg.isEmpty(),
                "no org named 'totally-fake-org-xyz' publishes copybook, so it must be reported as missing");
    }

    @Test(description = "A zero-count colliding module must not block fair-share pagination of a normal module")
    public void testSearchTypesByPackagesFairShareUnaffectedByCollidingZeroCountModule() {
        // ballerina/copybook resolves to 0 rows for its own org; ballerina/os has 5. The pair must still return
        // exactly os's 5 types, proving the join used for per-package counts isn't dropping or double-counting
        // rows because of the name collision.
        List<SearchResult> results = dbManager.searchTypesByPackages(
                Map.of("copybook", "ballerina", "os", "ballerina"), 100, 0);
        Assert.assertEquals(results.size(), 5);
        for (SearchResult result : results) {
            Assert.assertEquals(result.packageInfo().moduleName(), "os");
            Assert.assertEquals(result.packageInfo().org(), "ballerina");
        }
    }

    @Test(description = "countIndexedTypes must sum real per-module counts and ignore modules absent from the "
            + "index, matching the capacity TypeSearchCommand relies on to offset its live-fallback pagination")
    public void testCountIndexedTypesSumsAcrossModules() {
        // ballerinax/copybook=3, ballerina/np=3, ballerina/os=5 -> 11 total; a module with no index rows at all
        // contributes 0 rather than throwing or being skipped.
        int total = dbManager.countIndexedTypes(Map.of(
                "copybook", "ballerinax",
                "np", "ballerina",
                "os", "ballerina",
                "totally-fake-module-xyz", "totally-fake-org-xyz"));
        Assert.assertEquals(total, 11);

        Assert.assertEquals(dbManager.countIndexedTypes(Map.of()), 0);
    }
}
