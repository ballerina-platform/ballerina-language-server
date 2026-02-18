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

package io.ballerina.flowmodelgenerator.core.model;

import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static org.testng.Assert.*;

/**
 * Unit tests for {@link Member}.
 *
 * @since 1.0.0
 */
public class MemberTest {

    @Test(description = "Test Member record creation")
    public void testMemberCreation() {
        // Arrange & Act
        Member member = new Member(
                Member.MemberKind.FIELD,
                List.of("ref1", "ref2"),
                "string",
                "string",
                "name",
                "John Doe",
                false,
                false,
                false,
                "The name of the person",
                List.of(),
                Map.of("import1", "ballerina/http"),
                true
        );

        // Assert
        assertEquals(member.kind(), Member.MemberKind.FIELD);
        assertEquals(member.refs().size(), 2);
        assertEquals(member.type(), "string");
        assertEquals(member.typeName(), "string");
        assertEquals(member.name(), "name");
        assertEquals(member.defaultValue(), "John Doe");
        assertFalse(member.optional());
        assertFalse(member.readonly());
        assertFalse(member.isGraphqlId());
        assertEquals(member.docs(), "The name of the person");
        assertTrue(member.selected());
    }

    @Test(description = "Test Member with optional field")
    public void testOptionalMember() {
        // Arrange & Act
        Member member = new Member(
                Member.MemberKind.FIELD,
                null,
                "int",
                "int",
                "age",
                null,
                true,
                false,
                false,
                null,
                null,
                null,
                false
        );

        // Assert
        assertTrue(member.optional());
        assertNull(member.defaultValue());
        assertNull(member.refs());
    }

    @Test(description = "Test Member with readonly field")
    public void testReadonlyMember() {
        // Arrange & Act
        Member member = new Member(
                Member.MemberKind.FIELD,
                null,
                "string",
                null,
                "id",
                null,
                false,
                true,
                false,
                null,
                null,
                null,
                true
        );

        // Assert
        assertTrue(member.readonly());
        assertFalse(member.optional());
    }

    @Test(description = "Test Member with GraphQL ID")
    public void testGraphqlIdMember() {
        // Arrange & Act
        Member member = new Member(
                Member.MemberKind.FIELD,
                null,
                "string",
                null,
                "userId",
                null,
                false,
                false,
                true,
                null,
                null,
                null,
                false
        );

        // Assert
        assertTrue(member.isGraphqlId());
    }

    @Test(description = "Test Member with annotation attachments")
    public void testMemberWithAnnotations() {
        // Arrange
        AnnotationAttachment annotation = new AnnotationAttachment(
                "display",
                Map.of("label", "User Name")
        );

        // Act
        Member member = new Member(
                Member.MemberKind.FIELD,
                null,
                "string",
                null,
                "username",
                null,
                false,
                false,
                false,
                null,
                List.of(annotation),
                null,
                false
        );

        // Assert
        assertNotNull(member.annotationAttachments());
        assertEquals(member.annotationAttachments().size(), 1);
        assertEquals(member.annotationAttachments().get(0).name(), "display");
    }

    @Test(description = "Test MemberBuilder")
    public void testMemberBuilder() {
        // Arrange
        Member.MemberBuilder builder = new Member.MemberBuilder();

        // Act
        Member member = builder
                .kind(Member.MemberKind.FIELD)
                .type("int")
                .typeName("int")
                .name("count")
                .defaultValue("0")
                .optional(false)
                .readonly(false)
                .isGraphqlId(false)
                .docs("Count of items")
                .selected(true)
                .build();

        // Assert
        assertEquals(member.kind(), Member.MemberKind.FIELD);
        assertEquals(member.type(), "int");
        assertEquals(member.name(), "count");
        assertEquals(member.defaultValue(), "0");
        assertEquals(member.docs(), "Count of items");
        assertTrue(member.selected());
    }

    @Test(description = "Test MemberBuilder with refs")
    public void testMemberBuilderWithRefs() {
        // Arrange
        Member.MemberBuilder builder = new Member.MemberBuilder();

        // Act
        Member member = builder
                .kind(Member.MemberKind.TYPE)
                .refs(List.of("TypeRef1", "TypeRef2"))
                .type("CustomType")
                .name("customField")
                .build();

        // Assert
        assertNotNull(member.refs());
        assertEquals(member.refs().size(), 2);
        assertTrue(member.refs().contains("TypeRef1"));
        assertTrue(member.refs().contains("TypeRef2"));
    }

    @Test(description = "Test MemberBuilder with imports")
    public void testMemberBuilderWithImports() {
        // Arrange
        Member.MemberBuilder builder = new Member.MemberBuilder();
        Map<String, String> imports = Map.of(
                "http", "ballerina/http",
                "sql", "ballerina/sql"
        );

        // Act
        Member member = builder
                .kind(Member.MemberKind.FIELD)
                .type("http:Client")
                .name("httpClient")
                .imports(imports)
                .build();

        // Assert
        assertNotNull(member.imports());
        assertEquals(member.imports().size(), 2);
        assertEquals(member.imports().get("http"), "ballerina/http");
    }

    @Test(description = "Test Member toBuilder method")
    public void testMemberToBuilder() {
        // Arrange
        Member originalMember = new Member(
                Member.MemberKind.FIELD,
                List.of("ref"),
                "string",
                "string",
                "email",
                null,
                true,
                false,
                false,
                "Email address",
                null,
                null,
                false
        );

        // Act
        Member.MemberBuilder builder = originalMember.toBuilder();
        Member newMember = builder
                .defaultValue("test@example.com")
                .selected(true)
                .build();

        // Assert
        assertEquals(newMember.kind(), originalMember.kind());
        assertEquals(newMember.type(), originalMember.type());
        assertEquals(newMember.name(), originalMember.name());
        assertEquals(newMember.optional(), originalMember.optional());
        assertEquals(newMember.docs(), originalMember.docs());
        assertEquals(newMember.defaultValue(), "test@example.com");
        assertTrue(newMember.selected());
    }

    @Test(description = "Test Member getTypeAsTypeData with TypeData")
    public void testGetTypeAsTypeDataWithValidTypeData() {
        // Arrange
        TypeData typeData = new TypeData.TypeDataBuilder()
                .name("Person")
                .build();

        Member member = new Member(
                Member.MemberKind.FIELD,
                null,
                typeData,
                null,
                "person",
                null,
                false,
                false,
                false,
                null,
                null,
                null,
                false
        );

        // Act
        TypeData result = member.getTypeAsTypeData();

        // Assert
        assertNotNull(result);
        assertEquals(result.name(), "Person");
    }

    @Test(description = "Test Member getTypeAsTypeData with non-TypeData")
    public void testGetTypeAsTypeDataWithString() {
        // Arrange
        Member member = new Member(
                Member.MemberKind.FIELD,
                null,
                "string",
                null,
                "name",
                null,
                false,
                false,
                false,
                null,
                null,
                null,
                false
        );

        // Act
        TypeData result = member.getTypeAsTypeData();

        // Assert
        assertNull(result);
    }

    @Test(description = "Test MemberKind enum values")
    public void testMemberKindEnumValues() {
        // Assert
        assertEquals(Member.MemberKind.values().length, 3);
        assertNotNull(Member.MemberKind.valueOf("FIELD"));
        assertNotNull(Member.MemberKind.valueOf("TYPE"));
        assertNotNull(Member.MemberKind.valueOf("NAME"));
    }

    @Test(description = "Test MemberBuilder resets after build")
    public void testMemberBuilderResetsAfterBuild() {
        // Arrange
        Member.MemberBuilder builder = new Member.MemberBuilder();

        // Act
        Member firstMember = builder
                .kind(Member.MemberKind.FIELD)
                .type("string")
                .name("field1")
                .optional(true)
                .selected(true)
                .build();

        Member secondMember = builder
                .kind(Member.MemberKind.FIELD)
                .type("int")
                .name("field2")
                .build();

        // Assert
        assertEquals(firstMember.type(), "string");
        assertEquals(firstMember.name(), "field1");
        assertTrue(firstMember.optional());
        assertTrue(firstMember.selected());

        assertEquals(secondMember.type(), "int");
        assertEquals(secondMember.name(), "field2");
        assertFalse(secondMember.optional());
        assertFalse(secondMember.selected());
    }

    @Test(description = "Test Member with complex type object")
    public void testMemberWithComplexTypeObject() {
        // Arrange
        Map<String, Object> complexType = Map.of(
                "kind", "array",
                "elementType", "string"
        );

        // Act
        Member member = new Member(
                Member.MemberKind.FIELD,
                null,
                complexType,
                null,
                "items",
                null,
                false,
                false,
                false,
                null,
                null,
                null,
                false
        );

        // Assert
        assertNotNull(member.type());
        assertTrue(member.type() instanceof Map);
    }
}