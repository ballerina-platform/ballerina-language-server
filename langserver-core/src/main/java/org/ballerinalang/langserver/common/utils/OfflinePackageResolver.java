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

package org.ballerinalang.langserver.common.utils;

import io.ballerina.projects.Package;
import io.ballerina.projects.PackageDependencyScope;
import io.ballerina.projects.PackageDescriptor;
import io.ballerina.projects.PackageName;
import io.ballerina.projects.PackageOrg;
import io.ballerina.projects.PackageVersion;
import io.ballerina.projects.SemanticVersion;
import io.ballerina.projects.environment.PackageRepository;
import io.ballerina.projects.environment.ResolutionOptions;
import io.ballerina.projects.environment.ResolutionRequest;
import io.ballerina.projects.internal.environment.BallerinaUserHome;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Offline implementation of {@link PackageResolver} that resolves packages exclusively from
 * the local Ballerina user home repository. When the latest version is requested, it
 * searches the local cache and picks the highest semantic version available.
 *
 * @since 1.7.0
 */
final class OfflinePackageResolver   extends PackageResolver {

    private final PackageRepository userHomeRepository;

    OfflinePackageResolver() {
        this.userHomeRepository = BallerinaUserHome
                .from(sampleProject().projectEnvironmentContext().environment())
                .remotePackageRepository();
    }

    @Override
    protected Optional<Package> getModulePackage(String org, String name, String version) {
        ResolutionRequest request = ResolutionRequest.from(
                PackageDescriptor.from(PackageOrg.from(org), PackageName.from(name), PackageVersion.from(version)),
                PackageDependencyScope.DEFAULT);
        return userHomeRepository.getPackage(request, ResolutionOptions.builder().setOffline(true).build())
                .flatMap(pkg -> loadBalaPackage(pkg.project().sourceRoot()));
    }

    @Override
    protected Optional<Package> getModulePackage(String org, String name) {
        return getLatestVersion(userHomeRepository.getPackages(), org, name)
                .flatMap(version -> getModulePackage(org, name, version));
    }

    @Override
    protected boolean isModuleUnresolvedInternal(String org, String name, String version) {
        return getModulePackage(org, name, version).isEmpty();
    }

    @Override
    protected String fetchVersionIfNotExistsInternal(String org, String packageName, String version) {
        if (version != null) {
            return version;
        }

        return getLatestVersion(userHomeRepository.getPackages(), org, packageName).orElse(null);
    }

    private static Optional<String> getLatestVersion(Map<String, List<String>> packageMap, String org, String name) {
        return packageMap.getOrDefault(org, Collections.emptyList()).stream()
                .map(entry -> entry.split(":"))
                .filter(parts -> parts.length == 2 && name.equals(parts[0]))
                .map(parts -> parts[1])
                .max((versionOne, versionTwo) -> {
                    SemanticVersion semanticVersionOne = SemanticVersion.from(versionOne);
                    SemanticVersion semanticVersionTwo = SemanticVersion.from(versionTwo);
                    if (semanticVersionOne.greaterThan(semanticVersionTwo)) {
                        return 1;
                    } else if (semanticVersionTwo.greaterThan(semanticVersionOne)) {
                        return -1;
                    }
                    return 0;
                });
    }
}
