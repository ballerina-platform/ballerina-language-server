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

import static org.testng.Assert.*;

/**
 * Unit tests for {@link PropertyType}.
 *
 * @since 1.5.0
 */
public class PropertyTypeTest {

    @Test(description = "Test PropertyType creation with all fields")
    public void testPropertyTypeCreation() {
        // Arrange
        Property.ValueType fieldType = Property.ValueType.EXPRESSION;
        String ballerinaType = "string";
        String scope = "Local";
        List<Option> options = List.of();
        Property template = null;
        List<PropertyTypeMemberInfo> typeMembers = null;
        RecordSelectorType recordSelectorType = null;
        boolean selected = true;

        // Act
        PropertyType propertyType = new PropertyType(
                fieldType,
                ballerinaType,
                scope,
                options,
                template,
                typeMembers,
                recordSelectorType,
                selected
        );

        // Assert
        assertEquals(propertyType.fieldType(), fieldType);
        assertEquals(propertyType.ballerinaType(), "string");
        assertEquals(propertyType.scope(), "Local");
        assertNotNull(propertyType.options());
        assertTrue(propertyType.options().isEmpty());
        assertNull(propertyType.template());
        assertNull(propertyType.typeMembers());
        assertNull(propertyType.recordSelectorType());
        assertTrue(propertyType.selected());
    }

    @Test(description = "Test PropertyType with ballerinaType")
    public void testPropertyTypeWithBallerinaType() {
        // Arrange & Act
        PropertyType propertyType = new PropertyType(
                Property.ValueType.EXPRESSION,
                "int",
                null,
                null,
                null,
                null,
                null,
                false
        );

        // Assert
        assertEquals(propertyType.fieldType(), Property.ValueType.EXPRESSION);
        assertEquals(propertyType.ballerinaType(), "int");
        assertFalse(propertyType.selected());
    }

    @Test(description = "Test PropertyType with scope")
    public void testPropertyTypeWithScope() {
        // Arrange & Act
        PropertyType propertyType = new PropertyType(
                Property.ValueType.IDENTIFIER,
                null,
                "Global",
                null,
                null,
                null,
                null,
                true
        );

        // Assert
        assertEquals(propertyType.scope(), "Global");
        assertTrue(propertyType.selected());
    }

    @Test(description = "Test PropertyType with options")
    public void testPropertyTypeWithOptions() {
        // Arrange
        Option option1 = new Option("option1", "Option 1");
        Option option2 = new Option("option2", "Option 2");
        List<Option> options = List.of(option1, option2);

        // Act
        PropertyType propertyType = new PropertyType(
                Property.ValueType.SINGLE_SELECT,
                null,
                null,
                options,
                null,
                null,
                null,
                false
        );

        // Assert
        assertEquals(propertyType.fieldType(), Property.ValueType.SINGLE_SELECT);
        assertNotNull(propertyType.options());
        assertEquals(propertyType.options().size(), 2);
        assertEquals(propertyType.options().get(0).value(), "option1");
        assertEquals(propertyType.options().get(1).value(), "option2");
    }

    @Test(description = "Test PropertyType with template")
    public void testPropertyTypeWithTemplate() {
        // Arrange
        Property template = new Property(
                new Metadata("Label", "Doc"),
                null,
                "value",
                null,
                null,
                false,
                true,
                false,
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        // Act
        PropertyType propertyType = new PropertyType(
                Property.ValueType.FIXED_PROPERTY,
                null,
                null,
                null,
                template,
                null,
                null,
                true
        );

        // Assert
        assertNotNull(propertyType.template());
        assertEquals(propertyType.template().metadata().label(), "Label");
        assertEquals(propertyType.template().value(), "value");
    }

    @Test(description = "Test PropertyType with record selector type")
    public void testPropertyTypeWithRecordSelectorType() {
        // Arrange
        TypeData rootType = new TypeData.TypeDataBuilder().name("Person").build();
        RecordSelectorType recordSelectorType = new RecordSelectorType(rootType, List.of());

        // Act
        PropertyType propertyType = new PropertyType(
                Property.ValueType.TYPE,
                null,
                null,
                null,
                null,
                null,
                recordSelectorType,
                false
        );

        // Assert
        assertNotNull(propertyType.recordSelectorType());
        assertEquals(propertyType.recordSelectorType().rootType().name(), "Person");
    }

    @Test(description = "Test PropertyType selected setter")
    public void testPropertyTypeSelectedSetter() {
        // Arrange
        PropertyType propertyType = new PropertyType(
                Property.ValueType.EXPRESSION,
                "string",
                null,
                null,
                null,
                null,
                null,
                false
        );

        // Act
        assertFalse(propertyType.selected());
        propertyType.selected(true);

        // Assert
        assertTrue(propertyType.selected());
    }

    @Test(description = "Test PropertyType with all null optional fields")
    public void testPropertyTypeWithNullFields() {
        // Arrange & Act
        PropertyType propertyType = new PropertyType(
                Property.ValueType.TEXT,
                null,
                null,
                null,
                null,
                null,
                null,
                true
        );

        // Assert
        assertEquals(propertyType.fieldType(), Property.ValueType.TEXT);
        assertNull(propertyType.ballerinaType());
        assertNull(propertyType.scope());
        assertNull(propertyType.options());
        assertNull(propertyType.template());
        assertNull(propertyType.typeMembers());
        assertNull(propertyType.recordSelectorType());
        assertTrue(propertyType.selected());
    }

    @Test(description = "Test PropertyType equality")
    public void testPropertyTypeEquality() {
        // Arrange
        PropertyType propertyType1 = new PropertyType(
                Property.ValueType.EXPRESSION,
                "string",
                "Local",
                List.of(),
                null,
                null,
                null,
                true
        );

        PropertyType propertyType2 = new PropertyType(
                Property.ValueType.EXPRESSION,
                "string",
                "Local",
                List.of(),
                null,
                null,
                null,
                true
        );

        // Assert
        assertEquals(propertyType1.fieldType(), propertyType2.fieldType());
        assertEquals(propertyType1.ballerinaType(), propertyType2.ballerinaType());
        assertEquals(propertyType1.scope(), propertyType2.scope());
        assertEquals(propertyType1.selected(), propertyType2.selected());
    }

    @Test(description = "Test PropertyType toggle selected")
    public void testPropertyTypeToggleSelected() {
        // Arrange
        PropertyType propertyType = new PropertyType(
                Property.ValueType.FLAG,
                "boolean",
                null,
                null,
                null,
                null,
                null,
                false
        );

        // Act & Assert
        assertFalse(propertyType.selected());

        propertyType.selected(true);
        assertTrue(propertyType.selected());

        propertyType.selected(false);
        assertFalse(propertyType.selected());

        propertyType.selected(true);
        assertTrue(propertyType.selected());
    }
}