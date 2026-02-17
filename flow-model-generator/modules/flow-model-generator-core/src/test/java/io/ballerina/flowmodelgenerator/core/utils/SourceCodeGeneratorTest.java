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

package io.ballerina.flowmodelgenerator.core.utils;

import io.ballerina.flowmodelgenerator.core.model.AnnotationAttachment;
import io.ballerina.flowmodelgenerator.core.model.Codedata;
import io.ballerina.flowmodelgenerator.core.model.Function;
import io.ballerina.flowmodelgenerator.core.model.Member;
import io.ballerina.flowmodelgenerator.core.model.Metadata;
import io.ballerina.flowmodelgenerator.core.model.NodeKind;
import io.ballerina.flowmodelgenerator.core.model.Property;
import io.ballerina.flowmodelgenerator.core.model.TypeData;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Unit tests for SourceCodeGenerator.
 *
 *  @since 1.0.0
 */
public class SourceCodeGeneratorTest {

    private SourceCodeGenerator generator;

    @BeforeMethod
    public void setUp() {
        generator = new SourceCodeGenerator();
    }

    @Test
    public void testGenerateEnumCodeSnippet() {
        TypeData typeData = createTypeData(NodeKind.ENUM, "Status", List.of(
                new Member("ACTIVE", null, "ACTIVE", null, null, null, false, false, false, null, null, null),
                new Member("INACTIVE", null, "INACTIVE", null, null, null, false, false, false, null, null, null)
        ));

        String result = generator.generateCodeSnippetForType(typeData);

        assertNotNull(result);
        assertTrue(result.contains("enum Status"));
        assertTrue(result.contains("ACTIVE"));
        assertTrue(result.contains("INACTIVE"));
    }

    @Test
    public void testGenerateEnumWithValues() {
        TypeData typeData = createTypeData(NodeKind.ENUM, "Priority", List.of(
                new Member("HIGH", null, "HIGH", "1", null, null, false, false, false, null, null, null),
                new Member("MEDIUM", null, "MEDIUM", "2", null, null, false, false, false, null, null, null),
                new Member("LOW", null, "LOW", "3", null, null, false, false, false, null, null, null)
        ));

        String result = generator.generateCodeSnippetForType(typeData);

        assertNotNull(result);
        assertTrue(result.contains("HIGH = 1"));
        assertTrue(result.contains("MEDIUM = 2"));
        assertTrue(result.contains("LOW = 3"));
    }

    @Test
    public void testGeneratePublicEnum() {
        Map<String, Property> properties = new HashMap<>();
        Property publicProperty = createProperty(Property.ValueType.FLAG, true);
        properties.put(Property.IS_PUBLIC_KEY, publicProperty);

        TypeData typeData = new TypeData(
                "Status",
                new Metadata("Status", "Status enum", null, null, null),
                new Codedata(NodeKind.ENUM, null, null, null, null, null, null, null, null),
                List.of(new Member("ACTIVE", null, "ACTIVE", null, null, null, false, false, false, null, null, null)),
                null,
                null,
                null,
                false,
                properties
        );

        String result = generator.generateCodeSnippetForType(typeData);

        assertNotNull(result);
        assertTrue(result.contains("public enum Status"));
    }

    @Test
    public void testGenerateRecordTypeDescriptor() {
        TypeData typeData = createRecordType();
        String result = generator.generateCodeSnippetForType(typeData);

        assertNotNull(result);
        assertTrue(result.contains("record {|"));
        assertTrue(result.contains("string name;"));
        assertTrue(result.contains("int age;"));
        assertTrue(result.contains("|}"));
    }

    @Test
    public void testGenerateOpenRecordTypeDescriptor() {
        TypeData typeData = createOpenRecordType();
        String result = generator.generateCodeSnippetForType(typeData);

        assertNotNull(result);
        assertTrue(result.contains("record {"));
        assertTrue(result.contains("string name;"));
        assertTrue(result.contains("int age;"));
        assertTrue(result.contains("}"));
        assertFalse(result.contains("{|"));
    }

    @Test
    public void testGenerateArrayTypeDescriptor() {
        Member elementType = new Member(null, "string", null, null, null, null, false, false, false, null, null, null);
        TypeData arrayTypeData = new TypeData(
                null,
                null,
                new Codedata(NodeKind.ARRAY, null, null, null, null, null, null, null, null),
                List.of(elementType),
                null,
                null,
                null,
                false,
                new HashMap<>()
        );

        String result = generator.generateCodeSnippetForType(arrayTypeData);

        assertNotNull(result);
        assertTrue(result.contains("string[]"));
    }

    @Test
    public void testGenerateArrayWithSize() {
        Member elementType = new Member(null, "int", null, null, null, null, false, false, false, null, null, null);
        Map<String, Property> properties = new HashMap<>();
        Property arraySizeProperty = createProperty(Property.ValueType.TEXT, "5");
        properties.put(Property.ARRAY_SIZE, arraySizeProperty);

        TypeData arrayTypeData = new TypeData(
                null,
                null,
                new Codedata(NodeKind.ARRAY, null, null, null, null, null, null, null, null),
                List.of(elementType),
                null,
                null,
                null,
                false,
                properties
        );

        String result = generator.generateCodeSnippetForType(arrayTypeData);

        assertNotNull(result);
        assertTrue(result.contains("int[5]"));
    }

    @Test
    public void testGenerateMapTypeDescriptor() {
        Member valueType = new Member(null, "int", null, null, null, null, false, false, false, null, null, null);
        TypeData mapTypeData = new TypeData(
                null,
                null,
                new Codedata(NodeKind.MAP, null, null, null, null, null, null, null, null),
                List.of(valueType),
                null,
                null,
                null,
                false,
                new HashMap<>()
        );

        String result = generator.generateCodeSnippetForType(mapTypeData);

        assertNotNull(result);
        assertTrue(result.contains("map<int>"));
    }

    @Test
    public void testGenerateUnionTypeDescriptor() {
        Member stringType = new Member(null, "string", null, null, null, null, false, false, false, null, null, null);
        Member intType = new Member(null, "int", null, null, null, null, false, false, false, null, null, null);

        TypeData unionTypeData = new TypeData(
                null,
                null,
                new Codedata(NodeKind.UNION, null, null, null, null, null, null, null, null),
                List.of(stringType, intType),
                null,
                null,
                null,
                false,
                new HashMap<>()
        );

        String result = generator.generateCodeSnippetForType(unionTypeData);

        assertNotNull(result);
        assertTrue(result.contains("string|int"));
    }

    @Test
    public void testGenerateIntersectionTypeDescriptor() {
        Member readonlyType = new Member(null, "readonly", null, null, null, null, false, false, false, null, null, null);
        Member recordType = new Member(null, "Person", null, null, null, null, false, false, false, null, null, null);

        TypeData intersectionTypeData = new TypeData(
                null,
                null,
                new Codedata(NodeKind.INTERSECTION, null, null, null, null, null, null, null, null),
                List.of(readonlyType, recordType),
                null,
                null,
                null,
                false,
                new HashMap<>()
        );

        String result = generator.generateCodeSnippetForType(intersectionTypeData);

        assertNotNull(result);
        assertTrue(result.contains("readonly & Person"));
    }

    @Test
    public void testGenerateTupleTypeDescriptor() {
        Member stringType = new Member(null, "string", null, null, null, null, false, false, false, null, null, null);
        Member intType = new Member(null, "int", null, null, null, null, false, false, false, null, null, null);

        TypeData tupleTypeData = new TypeData(
                null,
                null,
                new Codedata(NodeKind.TUPLE, null, null, null, null, null, null, null, null),
                List.of(stringType, intType),
                null,
                null,
                null,
                false,
                new HashMap<>()
        );

        String result = generator.generateCodeSnippetForType(tupleTypeData);

        assertNotNull(result);
        assertTrue(result.contains("[string, int]"));
    }

    @Test
    public void testGenerateReadonlyType() {
        Map<String, Property> properties = new HashMap<>();
        Property readonlyProperty = createProperty(Property.ValueType.FLAG, "true");
        properties.put(Property.IS_READ_ONLY_KEY, readonlyProperty);

        TypeData typeData = new TypeData(
                "Person",
                new Metadata("Person", "Person type", null, null, null),
                new Codedata(NodeKind.RECORD, null, null, null, null, null, null, null, null),
                List.of(
                        new Member("name", "string", null, null, null, null, false, false, false, null, null, null)
                ),
                null,
                null,
                null,
                false,
                properties
        );

        String result = generator.generateCodeSnippetForType(typeData);

        assertNotNull(result);
        assertTrue(result.contains("readonly & record"));
    }

    @Test
    public void testGetImports() {
        Map<String, String> imports = generator.getImports();
        assertNotNull(imports);
        assertTrue(imports.isEmpty());
    }

    // Helper methods
    private TypeData createTypeData(NodeKind kind, String name, List<Member> members) {
        return new TypeData(
                name,
                new Metadata(name, null, null, null, null),
                new Codedata(kind, null, null, null, null, null, null, null, null),
                members,
                null,
                null,
                null,
                false,
                new HashMap<>()
        );
    }

    private TypeData createRecordType() {
        List<Member> members = List.of(
                new Member("name", "string", null, null, null, null, false, false, false, null, null, null),
                new Member("age", "int", null, null, null, null, false, false, false, null, null, null)
        );
        return new TypeData(
                "Person",
                new Metadata("Person", "Person record", null, null, null),
                new Codedata(NodeKind.RECORD, null, null, null, null, null, null, null, null),
                members,
                null,
                null,
                null,
                false,
                new HashMap<>()
        );
    }

    private TypeData createOpenRecordType() {
        List<Member> members = List.of(
                new Member("name", "string", null, null, null, null, false, false, false, null, null, null),
                new Member("age", "int", null, null, null, null, false, false, false, null, null, null)
        );
        return new TypeData(
                "Person",
                new Metadata("Person", "Person record", null, null, null),
                new Codedata(NodeKind.RECORD, null, null, null, null, null, null, null, null),
                members,
                null,
                null,
                null,
                true,  // allowAdditionalFields = true for open record
                new HashMap<>()
        );
    }

    private Property createProperty(Property.ValueType type, Object value) {
        return new Property(
                null,
                List.of(new Property.Type(type, null, null, null, null, false, null)),
                value,
                false,
                false,
                false,
                false,
                null,
                null,
                null,
                false,
                null
        );
    }
}