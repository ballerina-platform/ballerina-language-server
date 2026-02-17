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

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * Unit tests for FormBuilder.
 *
 * @since 1.0.0
 */
public class FormBuilderTest {

    private FormBuilder formBuilder;

    @BeforeMethod
    public void setUp() {
        formBuilder = new FormBuilder();
    }

    @Test
    public void testFormBuilderBasic() {
        Map<String, Property> properties = formBuilder.build();

        assertNotNull(properties);
        assertTrue(properties.isEmpty());
    }

    @Test
    public void testAddTextProperty() {
        formBuilder.addProperty("name", "John Doe", new Metadata("Name", "Enter name", null, null, null));
        Map<String, Property> properties = formBuilder.build();

        assertNotNull(properties);
        assertEquals(properties.size(), 1);
        assertTrue(properties.containsKey("name"));

        Property nameProperty = properties.get("name");
        assertEquals(nameProperty.value(), "John Doe");
        assertEquals(nameProperty.metadata().label(), "Name");
        assertFalse(nameProperty.optional());
        assertTrue(nameProperty.editable());
    }

    @Test
    public void testAddOptionalProperty() {
        formBuilder.addOptionalProperty("nickname", "Nick", new Metadata("Nickname", "Enter nickname", null, null, null));
        Map<String, Property> properties = formBuilder.build();

        Property nicknameProperty = properties.get("nickname");
        assertNotNull(nicknameProperty);
        assertTrue(nicknameProperty.optional());
    }

    @Test
    public void testAddAdvancedProperty() {
        formBuilder.addAdvancedProperty("advanced", "value", new Metadata("Advanced", "Advanced setting", null, null, null));
        Map<String, Property> properties = formBuilder.build();

        Property advancedProperty = properties.get("advanced");
        assertNotNull(advancedProperty);
        assertTrue(advancedProperty.advanced());
    }

    @Test
    public void testAddHiddenProperty() {
        formBuilder.addHiddenProperty("hidden", "secret", new Metadata("Hidden", "Hidden field", null, null, null));
        Map<String, Property> properties = formBuilder.build();

        Property hiddenProperty = properties.get("hidden");
        assertNotNull(hiddenProperty);
        assertTrue(hiddenProperty.hidden());
    }

    @Test
    public void testAddUneditableProperty() {
        formBuilder.addUneditableProperty("readonly", "fixed", new Metadata("Readonly", "Read only field", null, null, null));
        Map<String, Property> properties = formBuilder.build();

        Property readonlyProperty = properties.get("readonly");
        assertNotNull(readonlyProperty);
        assertFalse(readonlyProperty.editable());
    }

    @Test
    public void testAddVariableProperty() {
        formBuilder.addVariableProperty("varName", "myVar", new Metadata("Variable Name", "Enter variable name", null, null, null));
        Map<String, Property> properties = formBuilder.build();

        Property varProperty = properties.get("varName");
        assertNotNull(varProperty);
        assertEquals(varProperty.value(), "myVar");
        assertTrue(varProperty.types().get(0).fieldType() == Property.ValueType.IDENTIFIER ||
                varProperty.types().get(0).fieldType() == Property.ValueType.VARIABLE);
    }

    @Test
    public void testAddFlagProperty() {
        formBuilder.addFlagProperty("enabled", true, new Metadata("Enabled", "Enable feature", null, null, null));
        Map<String, Property> properties = formBuilder.build();

        Property flagProperty = properties.get("enabled");
        assertNotNull(flagProperty);
        assertEquals(flagProperty.value(), true);
        assertEquals(flagProperty.types().get(0).fieldType(), Property.ValueType.FLAG);
    }

    @Test
    public void testAddTypeProperty() {
        formBuilder.addTypeProperty("returnType", "string", new Metadata("Return Type", "Type of return value", null, null, null));
        Map<String, Property> properties = formBuilder.build();

        Property typeProperty = properties.get("returnType");
        assertNotNull(typeProperty);
        assertEquals(typeProperty.value(), "string");
        assertEquals(typeProperty.types().get(0).fieldType(), Property.ValueType.TYPE);
    }

    @Test
    public void testAddDocTextProperty() {
        formBuilder.addDocTextProperty("description", "This is a description", new Metadata("Description", "Enter description", null, null, null));
        Map<String, Property> properties = formBuilder.build();

        Property docProperty = properties.get("description");
        assertNotNull(docProperty);
        assertEquals(docProperty.value(), "This is a description");
        assertEquals(docProperty.types().get(0).fieldType(), Property.ValueType.DOC_TEXT);
    }

    @Test
    public void testAddExpressionProperty() {
        formBuilder.addExpressionProperty("expr", "a + b", new Metadata("Expression", "Enter expression", null, null, null));
        Map<String, Property> properties = formBuilder.build();

        Property exprProperty = properties.get("expr");
        assertNotNull(exprProperty);
        assertEquals(exprProperty.value(), "a + b");
        assertEquals(exprProperty.types().get(0).fieldType(), Property.ValueType.EXPRESSION);
    }

    @Test
    public void testAddMultipleProperties() {
        formBuilder.addProperty("name", "John", new Metadata("Name", null, null, null, null));
        formBuilder.addProperty("age", 30, new Metadata("Age", null, null, null, null));
        formBuilder.addFlagProperty("active", true, new Metadata("Active", null, null, null, null));

        Map<String, Property> properties = formBuilder.build();

        assertEquals(properties.size(), 3);
        assertTrue(properties.containsKey("name"));
        assertTrue(properties.containsKey("age"));
        assertTrue(properties.containsKey("active"));
    }

    @Test
    public void testOverwriteProperty() {
        formBuilder.addProperty("key", "value1", new Metadata("Key", null, null, null, null));
        formBuilder.addProperty("key", "value2", new Metadata("Key", null, null, null, null));

        Map<String, Property> properties = formBuilder.build();

        assertEquals(properties.size(), 1);
        assertEquals(properties.get("key").value(), "value2");
    }

    @Test
    public void testAddPropertyWithNullValue() {
        formBuilder.addProperty("nullable", null, new Metadata("Nullable", null, null, null, null));
        Map<String, Property> properties = formBuilder.build();

        assertTrue(properties.containsKey("nullable"));
        assertNull(properties.get("nullable").value());
    }

    @Test
    public void testAddPropertyWithEmptyString() {
        formBuilder.addProperty("empty", "", new Metadata("Empty", null, null, null, null));
        Map<String, Property> properties = formBuilder.build();

        assertTrue(properties.containsKey("empty"));
        assertEquals(properties.get("empty").value(), "");
    }

    @Test
    public void testAddPropertyWithCodedata() {
        Codedata codedata = new Codedata(NodeKind.VARIABLE, null, null, null, null, null, null, null, null);
        formBuilder.addProperty("withCodedata", "value", new Metadata("Label", null, null, null, null), codedata);
        Map<String, Property> properties = formBuilder.build();

        Property property = properties.get("withCodedata");
        assertNotNull(property);
        assertNotNull(property.codedata());
        assertEquals(property.codedata().node(), NodeKind.VARIABLE);
    }

    @Test
    public void testPropertyTypes() {
        formBuilder.addProperty("text", "value", new Metadata("Text", null, null, null, null));

        Map<String, Property> properties = formBuilder.build();
        Property property = properties.get("text");

        assertNotNull(property.types());
        assertFalse(property.types().isEmpty());
        assertEquals(property.types().get(0).fieldType(), Property.ValueType.TEXT);
    }

    @Test
    public void testAddComplexProperty() {
        Metadata metadata = new Metadata("Complex Property", "A complex property for testing",
                null, null, null);

        formBuilder.addProperty("complex", Map.of("key1", "value1", "key2", 123), metadata);
        Map<String, Property> properties = formBuilder.build();

        Property complexProperty = properties.get("complex");
        assertNotNull(complexProperty);
        assertTrue(complexProperty.value() instanceof Map);
    }

    @Test
    public void testReusingFormBuilder() {
        formBuilder.addProperty("prop1", "value1", new Metadata("Prop1", null, null, null, null));
        Map<String, Property> properties1 = formBuilder.build();

        // Try to add more properties after build
        formBuilder.addProperty("prop2", "value2", new Metadata("Prop2", null, null, null, null));
        Map<String, Property> properties2 = formBuilder.build();

        // Both builds should contain all properties added so far
        assertTrue(properties2.containsKey("prop1"));
        assertTrue(properties2.containsKey("prop2"));
    }

    @Test
    public void testAddScopedProperty() {
        formBuilder.addScopedProperty("scopedVar", "myVar", "Global",
                new Metadata("Scoped Variable", "Variable with scope", null, null, null));
        Map<String, Property> properties = formBuilder.build();

        Property scopedProperty = properties.get("scopedVar");
        assertNotNull(scopedProperty);
        assertEquals(scopedProperty.value(), "myVar");
        assertNotNull(scopedProperty.types());
        assertEquals(scopedProperty.types().get(0).scope(), "Global");
    }

    @Test
    public void testAddPropertyWithPriority() {
        formBuilder.addProperty("priority", "high", new Metadata("Priority", "Priority level", null, null, null));
        Map<String, Property> properties = formBuilder.build();

        Property priorityProperty = properties.get("priority");
        assertNotNull(priorityProperty);
        // Verify property was added successfully
        assertEquals(priorityProperty.value(), "high");
    }

    @Test
    public void testEmptyMetadata() {
        formBuilder.addProperty("noMeta", "value", null);
        Map<String, Property> properties = formBuilder.build();

        Property property = properties.get("noMeta");
        assertNotNull(property);
        // Metadata might be null or default
        assertEquals(property.value(), "value");
    }
}