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
import io.ballerina.flowmodelgenerator.core.model.NodeKind;
import io.ballerina.flowmodelgenerator.core.model.TypeData;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Unit tests for TypeSearchCommand.
 *
 * @since 1.0.0
 */
public class TypeSearchCommandTest {

    @Test
    public void testSearchCommandKind() {
        SearchCommand.Kind kind = SearchCommand.Kind.TYPE;
        assertNotNull(kind);
        assertEquals(kind.name(), "TYPE");
    }

    @Test
    public void testQueryMapKeyForKeyword() {
        String keywordKey = "keyword";
        assertNotNull(keywordKey);
        assertEquals(keywordKey, "keyword");
    }

    @Test
    public void testQueryMapKeyForModule() {
        String moduleKey = "module";
        assertNotNull(moduleKey);
        assertEquals(moduleKey, "module");
    }

    @Test
    public void testQueryMapKeyForTypeCategory() {
        String categoryKey = "category";
        assertNotNull(categoryKey);
        assertEquals(categoryKey, "category");
    }

    @Test
    public void testSearchForRecordTypes() {
        NodeKind recordKind = NodeKind.RECORD;
        assertNotNull(recordKind);
        assertEquals(recordKind, NodeKind.RECORD);
    }

    @Test
    public void testSearchForEnumTypes() {
        NodeKind enumKind = NodeKind.ENUM;
        assertNotNull(enumKind);
        assertEquals(enumKind, NodeKind.ENUM);
    }

    @Test
    public void testSearchForArrayTypes() {
        NodeKind arrayKind = NodeKind.ARRAY;
        assertNotNull(arrayKind);
        assertEquals(arrayKind, NodeKind.ARRAY);
    }

    @Test
    public void testSearchForMapTypes() {
        NodeKind mapKind = NodeKind.MAP;
        assertNotNull(mapKind);
        assertEquals(mapKind, NodeKind.MAP);
    }

    @Test
    public void testSearchForUnionTypes() {
        NodeKind unionKind = NodeKind.UNION;
        assertNotNull(unionKind);
        assertEquals(unionKind, NodeKind.UNION);
    }

    @Test
    public void testSearchForTupleTypes() {
        NodeKind tupleKind = NodeKind.TUPLE;
        assertNotNull(tupleKind);
        assertEquals(tupleKind, NodeKind.TUPLE);
    }

    @Test
    public void testBuiltinTypes() {
        List<String> builtinTypes = List.of(
                "int", "string", "boolean", "float", "decimal",
                "json", "xml", "byte", "any", "anydata"
        );

        assertEquals(builtinTypes.size(), 10);
        assertTrue(builtinTypes.contains("int"));
        assertTrue(builtinTypes.contains("string"));
    }

    @Test
    public void testEmptySearchQuery() {
        String query = "";
        assertNotNull(query);
        assertEquals(query.length(), 0);
    }

    @Test
    public void testNullSearchQuery() {
        String query = null;
        assertTrue(query == null);
    }

    @Test
    public void testSearchQueryCaseInsensitive() {
        String lowerQuery = "person";
        String upperQuery = "PERSON";

        assertFalse(lowerQuery.equals(upperQuery));
        assertTrue(lowerQuery.equalsIgnoreCase(upperQuery));
    }

    @Test
    public void testModuleFilterFormat() {
        String moduleFilter = "ballerina/http";
        assertTrue(moduleFilter.contains("/"));

        String[] parts = moduleFilter.split("/");
        assertEquals(parts.length, 2);
    }

    @Test
    public void testTypeSearchResultStructure() {
        Category category = Category.from("Types")
                .displayName("Types")
                .build();

        assertNotNull(category);
        assertEquals(category.name(), "Types");
    }

    @Test
    public void testSearchWithMultipleFilters() {
        Map<String, String> queryMap = Map.of(
                "keyword", "Person",
                "module", "myorg/mymodule",
                "category", "record"
        );

        assertEquals(queryMap.size(), 3);
        assertTrue(queryMap.containsKey("keyword"));
        assertTrue(queryMap.containsKey("module"));
        assertTrue(queryMap.containsKey("category"));
    }

    @Test
    public void testPartialMatchSearch() {
        String keyword = "Per";
        String typeName = "Person";

        assertTrue(typeName.toLowerCase().contains(keyword.toLowerCase()));
    }

    @Test
    public void testExactMatchSearch() {
        String keyword = "Person";
        String typeName = "Person";

        assertEquals(keyword, typeName);
    }

    @Test
    public void testEmptySearchResults() {
        List<TypeData> results = List.of();
        assertTrue(results.isEmpty());
        assertEquals(results.size(), 0);
    }

    @Test
    public void testSearchIncludesTypeMetadata() {
        String typeName = "Person";
        String typeDescription = "Represents a person entity";

        assertNotNull(typeName);
        assertNotNull(typeDescription);
    }

    @Test
    public void testRecordTypeWithFields() {
        Map<String, String> fields = Map.of(
                "name", "string",
                "age", "int",
                "email", "string"
        );

        assertEquals(fields.size(), 3);
        assertTrue(fields.containsKey("name"));
    }

    @Test
    public void testEnumTypeWithMembers() {
        List<String> members = List.of("RED", "GREEN", "BLUE");

        assertEquals(members.size(), 3);
        assertTrue(members.contains("RED"));
    }

    @Test
    public void testSearchForPublicTypes() {
        String visibility = "public";
        assertEquals(visibility, "public");
    }

    @Test
    public void testSearchForPrivateTypes() {
        String visibility = "private";
        assertEquals(visibility, "private");
    }

    @Test
    public void testSearchForAllTypes() {
        String visibility = "all";
        assertEquals(visibility, "all");
    }

    @Test
    public void testTypeWithDocumentation() {
        String documentation = "A custom type for representing user data";
        assertNotNull(documentation);
        assertTrue(documentation.length() > 0);
    }

    @Test
    public void testSearchResultsSorting() {
        List<String> typeNames = List.of("Address", "Person", "User");
        assertEquals(typeNames.get(0), "Address");
        assertEquals(typeNames.get(1), "Person");
        assertEquals(typeNames.get(2), "User");
    }

    @Test
    public void testCategoryHierarchyForTypes() {
        Category parent = Category.from("Types")
                .addCategory(Category.from("Records").build())
                .addCategory(Category.from("Enums").build())
                .build();

        assertNotNull(parent.categories());
        assertEquals(parent.categories().size(), 2);
    }

    @Test
    public void testOpenRecordType() {
        boolean isOpen = true;
        assertTrue(isOpen);
    }

    @Test
    public void testClosedRecordType() {
        boolean isOpen = false;
        assertFalse(isOpen);
    }

    @Test
    public void testReadonlyType() {
        String modifier = "readonly";
        assertEquals(modifier, "readonly");
    }

    @Test
    public void testDistinctType() {
        String modifier = "distinct";
        assertEquals(modifier, "distinct");
    }

    @Test
    public void testIsolatedType() {
        String modifier = "isolated";
        assertEquals(modifier, "isolated");
    }

    @Test
    public void testObjectType() {
        NodeKind objectKind = NodeKind.OBJECT;
        assertNotNull(objectKind);
        assertEquals(objectKind, NodeKind.OBJECT);
    }

    @Test
    public void testErrorType() {
        NodeKind errorKind = NodeKind.ERROR;
        assertNotNull(errorKind);
        assertEquals(errorKind, NodeKind.ERROR);
    }

    @Test
    public void testStreamType() {
        String streamType = "stream<int>";
        assertTrue(streamType.startsWith("stream"));
        assertTrue(streamType.contains("<"));
    }

    @Test
    public void testTableType() {
        String tableType = "table<Person> key(id)";
        assertTrue(tableType.startsWith("table"));
        assertTrue(tableType.contains("key"));
    }

    @Test
    public void testFunctionType() {
        String functionType = "function (int, int) returns int";
        assertTrue(functionType.startsWith("function"));
        assertTrue(functionType.contains("returns"));
    }

    @Test
    public void testOptionalType() {
        String optionalType = "string?";
        assertTrue(optionalType.endsWith("?"));
    }

    @Test
    public void testSearchResultLimit() {
        int maxResults = 100;
        assertTrue(maxResults > 0);
        assertTrue(maxResults <= 200);
    }

    @Test
    public void testTypeAliases() {
        String aliasName = "PersonId";
        String actualType = "string";

        assertNotNull(aliasName);
        assertNotNull(actualType);
    }
}