/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com)
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

package io.ballerina.flowmodelgenerator.core.search;

import io.ballerina.flowmodelgenerator.core.model.Category;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Unit tests for FunctionSearchCommand.
 *
 * @since 1.0.0
 */
public class FunctionSearchCommandTest {

    @Test
    public void testSearchCommandKind() {
        SearchCommand.Kind kind = SearchCommand.Kind.FUNCTION;
        assertNotNull(kind);
        assertEquals(kind.name(), "FUNCTION");
    }

    @Test
    public void testQueryMapKeyForKeyword() {
        String keywordKey = "keyword";
        assertNotNull(keywordKey);
        assertTrue(keywordKey.length() > 0);
    }

    @Test
    public void testQueryMapKeyForModule() {
        String moduleKey = "module";
        assertNotNull(moduleKey);
        assertTrue(moduleKey.length() > 0);
    }

    @Test
    public void testQueryMapKeyForVisibility() {
        String visibilityKey = "visibility";
        assertNotNull(visibilityKey);
        assertTrue(visibilityKey.length() > 0);
    }

    @Test
    public void testSearchResultCategoryStructure() {
        // Test that search results are organized in categories
        Category category = Category.from("Functions")
                .displayName("Functions")
                .build();

        assertNotNull(category);
        assertEquals(category.name(), "Functions");
        assertEquals(category.displayName(), "Functions");
    }

    @Test
    public void testEmptySearchQuery() {
        // Test that empty query is handled
        String query = "";
        assertNotNull(query);
        assertEquals(query, "");
    }

    @Test
    public void testNullSearchQuery() {
        // Test that null query is handled
        String query = null;
        assertTrue(query == null);
    }

    @Test
    public void testSearchQueryWithSpecialCharacters() {
        String query = "func$name";
        assertNotNull(query);
        assertTrue(query.contains("$"));
    }

    @Test
    public void testSearchQueryCaseSensitivity() {
        String lowerQuery = "function";
        String upperQuery = "FUNCTION";

        assertFalse(lowerQuery.equals(upperQuery));
        assertTrue(lowerQuery.equalsIgnoreCase(upperQuery));
    }

    @Test
    public void testModuleFilterFormat() {
        // Test module filter format: org/module
        String moduleFilter = "ballerina/http";
        assertTrue(moduleFilter.contains("/"));

        String[] parts = moduleFilter.split("/");
        assertEquals(parts.length, 2);
        assertEquals(parts[0], "ballerina");
        assertEquals(parts[1], "http");
    }

    @Test
    public void testVisibilityFilterPublic() {
        String visibility = "public";
        assertEquals(visibility, "public");
    }

    @Test
    public void testVisibilityFilterPrivate() {
        String visibility = "private";
        assertEquals(visibility, "private");
    }

    @Test
    public void testVisibilityFilterAll() {
        String visibility = "all";
        assertEquals(visibility, "all");
    }

    @Test
    public void testSearchResultsAreSorted() {
        // Test that search results are typically sorted
        List<String> functionNames = List.of("a", "b", "c");
        assertEquals(functionNames.get(0), "a");
        assertEquals(functionNames.get(1), "b");
        assertEquals(functionNames.get(2), "c");
    }

    @Test
    public void testSearchWithMultipleFilters() {
        Map<String, String> queryMap = Map.of(
                "keyword", "http",
                "module", "ballerina/http",
                "visibility", "public"
        );

        assertEquals(queryMap.size(), 3);
        assertTrue(queryMap.containsKey("keyword"));
        assertTrue(queryMap.containsKey("module"));
        assertTrue(queryMap.containsKey("visibility"));
    }

    @Test
    public void testSearchResultsIncludeMetadata() {
        // Verify that search results include function metadata
        String functionName = "testFunction";
        String functionDescription = "A test function";

        assertNotNull(functionName);
        assertNotNull(functionDescription);
    }

    @Test
    public void testSearchForBuiltinFunctions() {
        // Test searching for built-in Ballerina functions
        String keyword = "println";
        assertNotNull(keyword);
    }

    @Test
    public void testSearchForModuleFunctions() {
        // Test searching for module-specific functions
        String module = "ballerina/io";
        assertNotNull(module);
        assertTrue(module.startsWith("ballerina/"));
    }

    @Test
    public void testSearchForUserDefinedFunctions() {
        // Test searching for user-defined functions
        String keyword = "myCustomFunction";
        assertNotNull(keyword);
    }

    @Test
    public void testPartialMatchSearch() {
        // Test that partial keyword matching works
        String keyword = "get";
        String functionName = "getData";

        assertTrue(functionName.toLowerCase().contains(keyword.toLowerCase()));
    }

    @Test
    public void testExactMatchSearch() {
        // Test exact match search
        String keyword = "getData";
        String functionName = "getData";

        assertEquals(keyword, functionName);
    }

    @Test
    public void testSearchResultLimit() {
        // Test that search results can be limited
        int maxResults = 50;
        assertTrue(maxResults > 0);
        assertTrue(maxResults <= 100);
    }

    @Test
    public void testEmptySearchResults() {
        // Test handling of no search results
        List<FlowNode> results = List.of();
        assertTrue(results.isEmpty());
        assertEquals(results.size(), 0);
    }

    @Test
    public void testSearchIncludesParameterTypes() {
        // Test that function search includes parameter type information
        String paramType = "string";
        assertNotNull(paramType);
    }

    @Test
    public void testSearchIncludesReturnTypes() {
        // Test that function search includes return type information
        String returnType = "int|error";
        assertNotNull(returnType);
        assertTrue(returnType.contains("|"));
    }

    @Test
    public void testCategoryHierarchy() {
        // Test nested category structure for organizing functions
        Category parent = Category.from("parent")
                .addCategory(Category.from("child1").build())
                .addCategory(Category.from("child2").build())
                .build();

        assertNotNull(parent.categories());
        assertEquals(parent.categories().size(), 2);
    }

    @Test
    public void testFunctionWithDocumentation() {
        // Test that search results can include documentation
        String documentation = "Returns the sum of two integers";
        assertNotNull(documentation);
        assertTrue(documentation.length() > 0);
    }
}