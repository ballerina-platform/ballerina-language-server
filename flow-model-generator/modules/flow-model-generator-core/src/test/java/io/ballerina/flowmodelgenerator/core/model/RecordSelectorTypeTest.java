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

package io.ballerina.flowmodelgenerator.core.model;

import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.*;

/**
 * Unit tests for {@link RecordSelectorType}.
 *
 * @since 1.0.0
 */
public class RecordSelectorTypeTest {

    @Test(description = "Test RecordSelectorType creation")
    public void testRecordSelectorTypeCreation() {
        // Arrange
        TypeData rootType = new TypeData.TypeDataBuilder()
                .name("Person")
                .build();
        List<TypeData> referencedTypes = new ArrayList<>();

        // Act
        RecordSelectorType recordSelectorType = new RecordSelectorType(rootType, referencedTypes);

        // Assert
        assertNotNull(recordSelectorType);
        assertEquals(recordSelectorType.rootType().name(), "Person");
        assertNotNull(recordSelectorType.referencedTypes());
        assertTrue(recordSelectorType.referencedTypes().isEmpty());
    }

    @Test(description = "Test RecordSelectorType with referenced types")
    public void testRecordSelectorTypeWithReferencedTypes() {
        // Arrange
        TypeData rootType = new TypeData.TypeDataBuilder()
                .name("Employee")
                .build();

        TypeData addressType = new TypeData.TypeDataBuilder()
                .name("Address")
                .build();

        TypeData departmentType = new TypeData.TypeDataBuilder()
                .name("Department")
                .build();

        List<TypeData> referencedTypes = List.of(addressType, departmentType);

        // Act
        RecordSelectorType recordSelectorType = new RecordSelectorType(rootType, referencedTypes);

        // Assert
        assertEquals(recordSelectorType.rootType().name(), "Employee");
        assertEquals(recordSelectorType.referencedTypes().size(), 2);
        assertEquals(recordSelectorType.referencedTypes().get(0).name(), "Address");
        assertEquals(recordSelectorType.referencedTypes().get(1).name(), "Department");
    }

    @Test(description = "Test RecordSelectorType with null referenced types")
    public void testRecordSelectorTypeWithNullReferencedTypes() {
        // Arrange
        TypeData rootType = new TypeData.TypeDataBuilder()
                .name("Simple")
                .build();

        // Act
        RecordSelectorType recordSelectorType = new RecordSelectorType(rootType, null);

        // Assert
        assertNotNull(recordSelectorType);
        assertEquals(recordSelectorType.rootType().name(), "Simple");
        assertNull(recordSelectorType.referencedTypes());
    }

    @Test(description = "Test RecordSelectorType with complex root type")
    public void testRecordSelectorTypeWithComplexRootType() {
        // Arrange
        Member member1 = new Member(
                Member.MemberKind.FIELD,
                null,
                "string",
                "string",
                "name",
                null,
                false,
                false,
                false,
                "Name field",
                null,
                null,
                true
        );

        Member member2 = new Member(
                Member.MemberKind.FIELD,
                null,
                "int",
                "int",
                "age",
                null,
                false,
                false,
                false,
                "Age field",
                null,
                null,
                true
        );

        TypeData rootType = new TypeData.TypeDataBuilder()
                .name("Person")
                .members(List.of(member1, member2))
                .build();

        List<TypeData> referencedTypes = new ArrayList<>();

        // Act
        RecordSelectorType recordSelectorType = new RecordSelectorType(rootType, referencedTypes);

        // Assert
        assertEquals(recordSelectorType.rootType().name(), "Person");
        assertNotNull(recordSelectorType.rootType().members());
        assertEquals(recordSelectorType.rootType().members().size(), 2);
        assertEquals(recordSelectorType.rootType().members().get(0).name(), "name");
        assertEquals(recordSelectorType.rootType().members().get(1).name(), "age");
    }

    @Test(description = "Test RecordSelectorType record accessors")
    public void testRecordSelectorTypeAccessors() {
        // Arrange
        TypeData rootType = new TypeData.TypeDataBuilder()
                .name("RootType")
                .build();

        TypeData refType = new TypeData.TypeDataBuilder()
                .name("RefType")
                .build();

        List<TypeData> referencedTypes = List.of(refType);

        // Act
        RecordSelectorType recordSelectorType = new RecordSelectorType(rootType, referencedTypes);

        // Assert - Test that accessors work correctly
        assertSame(recordSelectorType.rootType(), rootType);
        assertSame(recordSelectorType.referencedTypes(), referencedTypes);
        assertSame(recordSelectorType.referencedTypes().get(0), refType);
    }

    @Test(description = "Test RecordSelectorType with nested types")
    public void testRecordSelectorTypeWithNestedTypes() {
        // Arrange
        TypeData addressType = new TypeData.TypeDataBuilder()
                .name("Address")
                .members(List.of(
                        new Member(Member.MemberKind.FIELD, null, "string", "string",
                                "street", null, false, false, false, null, null, null, true),
                        new Member(Member.MemberKind.FIELD, null, "string", "string",
                                "city", null, false, false, false, null, null, null, true)
                ))
                .build();

        TypeData rootType = new TypeData.TypeDataBuilder()
                .name("Person")
                .members(List.of(
                        new Member(Member.MemberKind.FIELD, null, "string", "string",
                                "name", null, false, false, false, null, null, null, true),
                        new Member(Member.MemberKind.FIELD, null, "Address", "Address",
                                "address", null, false, false, false, null, null, null, true)
                ))
                .build();

        List<TypeData> referencedTypes = List.of(addressType);

        // Act
        RecordSelectorType recordSelectorType = new RecordSelectorType(rootType, referencedTypes);

        // Assert
        assertEquals(recordSelectorType.rootType().name(), "Person");
        assertEquals(recordSelectorType.referencedTypes().size(), 1);
        assertEquals(recordSelectorType.referencedTypes().get(0).name(), "Address");
        assertEquals(recordSelectorType.referencedTypes().get(0).members().size(), 2);
    }

    @Test(description = "Test RecordSelectorType immutability considerations")
    public void testRecordSelectorTypeImmutability() {
        // Arrange
        TypeData rootType = new TypeData.TypeDataBuilder()
                .name("ImmutableTest")
                .build();

        List<TypeData> referencedTypes = new ArrayList<>();
        referencedTypes.add(new TypeData.TypeDataBuilder().name("Ref1").build());

        // Act
        RecordSelectorType recordSelectorType = new RecordSelectorType(rootType, referencedTypes);

        // Try to modify the original list
        referencedTypes.add(new TypeData.TypeDataBuilder().name("Ref2").build());

        // Assert - The record selector's referenced types should reflect the change
        // since records don't create defensive copies by default
        assertEquals(recordSelectorType.referencedTypes().size(), 2);
    }

    @Test(description = "Test RecordSelectorType equality")
    public void testRecordSelectorTypeEquality() {
        // Arrange
        TypeData rootType1 = new TypeData.TypeDataBuilder().name("Person").build();
        TypeData rootType2 = new TypeData.TypeDataBuilder().name("Person").build();
        List<TypeData> refs1 = List.of();
        List<TypeData> refs2 = List.of();

        // Act
        RecordSelectorType rst1 = new RecordSelectorType(rootType1, refs1);
        RecordSelectorType rst2 = new RecordSelectorType(rootType2, refs2);

        // Assert
        assertEquals(rst1.rootType().name(), rst2.rootType().name());
        assertEquals(rst1.referencedTypes().size(), rst2.referencedTypes().size());
    }
}