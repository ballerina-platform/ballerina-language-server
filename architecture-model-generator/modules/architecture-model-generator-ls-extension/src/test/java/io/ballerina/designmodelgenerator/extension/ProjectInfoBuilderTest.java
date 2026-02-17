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

package io.ballerina.designmodelgenerator.extension;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.designmodelgenerator.extension.response.ProjectInfoResponse;
import io.ballerina.projects.BuildProject;
import io.ballerina.projects.Document;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Module;
import io.ballerina.projects.ModuleName;
import io.ballerina.projects.Package;
import io.ballerina.projects.PackageName;
import io.ballerina.projects.PackageOrg;
import io.ballerina.projects.PackageVersion;
import io.ballerina.projects.Project;
import io.ballerina.projects.ProjectKind;
import io.ballerina.projects.directory.BuildProject;
import io.ballerina.projects.directory.WorkspaceProject;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * Unit tests for ProjectInfoBuilder.
 *
 * @since 1.4.2
 */
public class ProjectInfoBuilderTest {

    @Test
    public void testFormatNameToTitleSnakeCase() {
        String result = invokeFormatNameToTitle("simple_project");
        assertEquals(result, "Simple Project");
    }

    @Test
    public void testFormatNameToTitleCamelCase() {
        String result = invokeFormatNameToTitle("simpleProject");
        assertEquals(result, "Simple Project");
    }

    @Test
    public void testFormatNameToTitleMixedCase() {
        String result = invokeFormatNameToTitle("simple_projectTest");
        assertEquals(result, "Simple Project Test");
    }

    @Test
    public void testFormatNameToTitleSingleWord() {
        String result = invokeFormatNameToTitle("project");
        assertEquals(result, "Project");
    }

    @Test
    public void testFormatNameToTitleEmpty() {
        String result = invokeFormatNameToTitle("");
        assertEquals(result, "");
    }

    @Test
    public void testFormatNameToTitleNull() {
        String result = invokeFormatNameToTitle(null);
        assertNull(result);
    }

    @Test
    public void testFormatNameToTitleMultipleUnderscores() {
        String result = invokeFormatNameToTitle("my__test___project");
        // Multiple underscores create empty segments which are filtered out
        assertTrue(result.contains("My") && result.contains("Test") && result.contains("Project"));
    }

    @Test
    public void testFormatNameToTitleWithNumbers() {
        String result = invokeFormatNameToTitle("project123Test");
        assertEquals(result, "Project123 Test");
    }

    @Test
    public void testFormatNameToTitleAllCaps() {
        String result = invokeFormatNameToTitle("PROJECT");
        assertEquals(result, "PROJECT");
    }

    @Test
    public void testSplitCamelCaseSimple() {
        List<String> result = invokeSplitCamelCase("simpleTest");
        assertEquals(result.size(), 2);
        assertEquals(result.get(0), "simple");
        assertEquals(result.get(1), "Test");
    }

    @Test
    public void testSplitCamelCaseMultiple() {
        List<String> result = invokeSplitCamelCase("thisIsATest");
        assertEquals(result.size(), 4);
        assertEquals(result.get(0), "this");
        assertEquals(result.get(1), "Is");
        assertEquals(result.get(2), "A");
        assertEquals(result.get(3), "Test");
    }

    @Test
    public void testSplitCamelCaseNoUpperCase() {
        List<String> result = invokeSplitCamelCase("test");
        assertEquals(result.size(), 1);
        assertEquals(result.get(0), "test");
    }

    @Test
    public void testSplitCamelCaseEmpty() {
        List<String> result = invokeSplitCamelCase("");
        assertTrue(result.isEmpty());
    }

    @Test
    public void testSplitCamelCaseNull() {
        List<String> result = invokeSplitCamelCase(null);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testCapitalizeSimple() {
        String result = invokeCapitalize("hello");
        assertEquals(result, "Hello");
    }

    @Test
    public void testCapitalizeUpperCase() {
        String result = invokeCapitalize("HELLO");
        assertEquals(result, "HELLO");
    }

    @Test
    public void testCapitalizeSingleChar() {
        String result = invokeCapitalize("a");
        assertEquals(result, "A");
    }

    @Test
    public void testCapitalizeEmpty() {
        String result = invokeCapitalize("");
        assertEquals(result, "");
    }

    @Test
    public void testCapitalizeNull() {
        String result = invokeCapitalize(null);
        assertNull(result);
    }

    @Test
    public void testCapitalizeMixedCase() {
        String result = invokeCapitalize("hELLO");
        assertEquals(result, "HELLO");
    }

    // Helper methods to invoke private static methods using reflection
    private String invokeFormatNameToTitle(String name) {
        try {
            java.lang.reflect.Method method = ProjectInfoBuilder.class.getDeclaredMethod("formatNameToTitle", String.class);
            method.setAccessible(true);
            return (String) method.invoke(null, name);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke formatNameToTitle", e);
        }
    }

    private List<String> invokeSplitCamelCase(String text) {
        try {
            java.lang.reflect.Method method = ProjectInfoBuilder.class.getDeclaredMethod("splitCamelCase", String.class);
            method.setAccessible(true);
            return (List<String>) method.invoke(null, text);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke splitCamelCase", e);
        }
    }

    private String invokeCapitalize(String word) {
        try {
            java.lang.reflect.Method method = ProjectInfoBuilder.class.getDeclaredMethod("capitalize", String.class);
            method.setAccessible(true);
            return (String) method.invoke(null, word);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke capitalize", e);
        }
    }
}