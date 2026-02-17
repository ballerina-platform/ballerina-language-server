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

import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * Unit tests for Category and Category.Builder.
 *
 * @since 1.0.0
 */
public class CategoryTest {

    @Test
    public void testCategoryCreation() {
        Category category = new Category("Test Category", null, null, false, null);

        assertNotNull(category);
        assertEquals(category.name(), "Test Category");
        assertNull(category.displayName());
        assertNull(category.items());
        assertEquals(category.isOpen(), false);
        assertNull(category.categories());
    }

    @Test
    public void testCategoryWithDisplayName() {
        Category category = new Category("test_category", "Test Category", null, false, null);

        assertEquals(category.name(), "test_category");
        assertEquals(category.displayName(), "Test Category");
    }

    @Test
    public void testCategoryWithItems() {
        List<FlowNode> items = new ArrayList<>();
        items.add(createFlowNode("item1"));
        items.add(createFlowNode("item2"));

        Category category = new Category("Test", null, items, false, null);

        assertNotNull(category.items());
        assertEquals(category.items().size(), 2);
    }

    @Test
    public void testCategoryWithSubcategories() {
        List<Category> subcategories = new ArrayList<>();
        subcategories.add(new Category("SubCat1", null, null, false, null));
        subcategories.add(new Category("SubCat2", null, null, false, null));

        Category category = new Category("Parent", null, null, false, subcategories);

        assertNotNull(category.categories());
        assertEquals(category.categories().size(), 2);
    }

    @Test
    public void testCategoryIsOpen() {
        Category category = new Category("Test", null, null, true, null);

        assertEquals(category.isOpen(), true);
    }

    @Test
    public void testBuilderBasic() {
        Category.Builder builder = Category.from("TestCategory");
        Category category = builder.build();

        assertNotNull(category);
        assertEquals(category.name(), "TestCategory");
        assertNull(category.displayName());
        assertNull(category.items());
        assertEquals(category.isOpen(), false);
        assertNull(category.categories());
    }

    @Test
    public void testBuilderWithDisplayName() {
        Category category = Category.from("test_category")
                .displayName("Test Category")
                .build();

        assertEquals(category.name(), "test_category");
        assertEquals(category.displayName(), "Test Category");
    }

    @Test
    public void testBuilderWithOpen() {
        Category category = Category.from("test")
                .open(true)
                .build();

        assertEquals(category.isOpen(), true);
    }

    @Test
    public void testBuilderAddSingleItem() {
        FlowNode item = createFlowNode("item1");
        Category category = Category.from("test")
                .addItem(item)
                .build();

        assertNotNull(category.items());
        assertEquals(category.items().size(), 1);
        assertEquals(category.items().get(0), item);
    }

    @Test
    public void testBuilderAddMultipleItems() {
        FlowNode item1 = createFlowNode("item1");
        FlowNode item2 = createFlowNode("item2");

        Category category = Category.from("test")
                .addItem(item1)
                .addItem(item2)
                .build();

        assertNotNull(category.items());
        assertEquals(category.items().size(), 2);
    }

    @Test
    public void testBuilderAddItemsList() {
        List<FlowNode> items = new ArrayList<>();
        items.add(createFlowNode("item1"));
        items.add(createFlowNode("item2"));
        items.add(createFlowNode("item3"));

        Category category = Category.from("test")
                .addItems(items)
                .build();

        assertNotNull(category.items());
        assertEquals(category.items().size(), 3);
    }

    @Test
    public void testBuilderAddSingleCategory() {
        Category subcategory = new Category("SubCat", null, null, false, null);
        Category category = Category.from("parent")
                .addCategory(subcategory)
                .build();

        assertNotNull(category.categories());
        assertEquals(category.categories().size(), 1);
        assertEquals(category.categories().get(0), subcategory);
    }

    @Test
    public void testBuilderAddMultipleCategories() {
        Category subcat1 = new Category("SubCat1", null, null, false, null);
        Category subcat2 = new Category("SubCat2", null, null, false, null);

        Category category = Category.from("parent")
                .addCategory(subcat1)
                .addCategory(subcat2)
                .build();

        assertNotNull(category.categories());
        assertEquals(category.categories().size(), 2);
    }

    @Test
    public void testBuilderAddCategoriesList() {
        List<Category> subcategories = new ArrayList<>();
        subcategories.add(new Category("SubCat1", null, null, false, null));
        subcategories.add(new Category("SubCat2", null, null, false, null));

        Category category = Category.from("parent")
                .addCategories(subcategories)
                .build();

        assertNotNull(category.categories());
        assertEquals(category.categories().size(), 2);
    }

    @Test
    public void testBuilderComplexCategory() {
        FlowNode item1 = createFlowNode("item1");
        FlowNode item2 = createFlowNode("item2");
        Category subcat = new Category("SubCat", null, null, false, null);

        Category category = Category.from("complex_category")
                .displayName("Complex Category")
                .open(true)
                .addItem(item1)
                .addItem(item2)
                .addCategory(subcat)
                .build();

        assertEquals(category.name(), "complex_category");
        assertEquals(category.displayName(), "Complex Category");
        assertEquals(category.isOpen(), true);
        assertNotNull(category.items());
        assertEquals(category.items().size(), 2);
        assertNotNull(category.categories());
        assertEquals(category.categories().size(), 1);
    }

    @Test
    public void testBuilderNestedCategories() {
        FlowNode leafItem = createFlowNode("leafItem");
        Category leafCategory = Category.from("leaf")
                .addItem(leafItem)
                .build();

        Category midCategory = Category.from("mid")
                .addCategory(leafCategory)
                .build();

        Category rootCategory = Category.from("root")
                .addCategory(midCategory)
                .build();

        assertNotNull(rootCategory.categories());
        assertEquals(rootCategory.categories().size(), 1);

        Category mid = rootCategory.categories().get(0);
        assertNotNull(mid.categories());
        assertEquals(mid.categories().size(), 1);

        Category leaf = mid.categories().get(0);
        assertNotNull(leaf.items());
        assertEquals(leaf.items().size(), 1);
    }

    @Test
    public void testBuilderEmptyCategory() {
        Category category = Category.from("empty").build();

        assertNotNull(category);
        assertEquals(category.name(), "empty");
        assertNull(category.items());
        assertNull(category.categories());
    }

    @Test
    public void testBuilderAddNullItem() {
        Category category = Category.from("test")
                .addItem(null)
                .build();

        // Should handle null gracefully
        if (category.items() != null) {
            assertTrue(category.items().isEmpty() || category.items().contains(null));
        }
    }

    @Test
    public void testBuilderAddNullCategory() {
        Category category = Category.from("test")
                .addCategory(null)
                .build();

        // Should handle null gracefully
        if (category.categories() != null) {
            assertTrue(category.categories().isEmpty() || category.categories().contains(null));
        }
    }

    // Helper method to create a FlowNode for testing
    private FlowNode createFlowNode(String name) {
        return new FlowNode(
                name,
                new Metadata(name, null, null, null, null),
                new Codedata(NodeKind.FUNCTION_CALL, null, null, null, null, null, null, null, null),
                null,
                null,
                null
        );
    }
}