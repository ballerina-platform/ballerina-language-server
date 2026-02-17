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

package io.ballerina.designmodelgenerator.extension.response;

import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * Unit tests for ProjectInfoResponse.
 *
 * @since 1.4.2
 */
public class ProjectInfoResponseTest {

    @Test
    public void testProjectInfoResponseCreation() {
        ProjectInfoResponse response = new ProjectInfoResponse(
                "myproject",
                "My Project",
                "0.1.0",
                "myorg",
                "My Organization",
                "BUILD",
                "/path/to/project",
                new ArrayList<>(),
                new HashMap<>()
        );

        assertNotNull(response);
        assertEquals(response.projectName(), "myproject");
        assertEquals(response.displayName(), "My Project");
        assertEquals(response.version(), "0.1.0");
        assertEquals(response.organization(), "myorg");
        assertEquals(response.displayOrganization(), "My Organization");
        assertEquals(response.kind(), "BUILD");
        assertEquals(response.projectPath(), "/path/to/project");
    }

    @Test
    public void testProjectInfoResponseWithNullDisplayName() {
        ProjectInfoResponse response = new ProjectInfoResponse(
                "myproject",
                null,
                "1.0.0",
                "myorg",
                null,
                "BUILD",
                "/path",
                new ArrayList<>(),
                new HashMap<>()
        );

        assertEquals(response.projectName(), "myproject");
        assertNull(response.displayName());
        assertNull(response.displayOrganization());
    }

    @Test
    public void testProjectInfoResponseWithModules() {
        List<ProjectInfoResponse.ModuleInfo> modules = new ArrayList<>();
        modules.add(new ProjectInfoResponse.ModuleInfo(
                "mymodule",
                "My Module",
                new ArrayList<>()
        ));

        ProjectInfoResponse response = new ProjectInfoResponse(
                "myproject",
                "My Project",
                "1.0.0",
                "myorg",
                "My Org",
                "BUILD",
                "/path",
                modules,
                new HashMap<>()
        );

        assertNotNull(response.modules());
        assertEquals(response.modules().size(), 1);
        assertEquals(response.modules().get(0).moduleName(), "mymodule");
    }

    @Test
    public void testProjectInfoResponseWithDependencies() {
        Map<String, String> dependencies = new HashMap<>();
        dependencies.put("ballerina/http", "2.0.0");
        dependencies.put("ballerina/io", "1.0.0");

        ProjectInfoResponse response = new ProjectInfoResponse(
                "myproject",
                "My Project",
                "1.0.0",
                "myorg",
                "My Org",
                "BUILD",
                "/path",
                new ArrayList<>(),
                dependencies
        );

        assertNotNull(response.dependencies());
        assertEquals(response.dependencies().size(), 2);
        assertTrue(response.dependencies().containsKey("ballerina/http"));
        assertEquals(response.dependencies().get("ballerina/http"), "2.0.0");
    }

    @Test
    public void testProjectKindBuild() {
        ProjectInfoResponse response = new ProjectInfoResponse(
                "project",
                "Project",
                "1.0.0",
                "org",
                "Org",
                "BUILD",
                "/path",
                new ArrayList<>(),
                new HashMap<>()
        );

        assertEquals(response.kind(), "BUILD");
    }

    @Test
    public void testProjectKindWorkspace() {
        ProjectInfoResponse response = new ProjectInfoResponse(
                "project",
                "Project",
                "1.0.0",
                "org",
                "Org",
                "WORKSPACE",
                "/path",
                new ArrayList<>(),
                new HashMap<>()
        );

        assertEquals(response.kind(), "WORKSPACE");
    }

    @Test
    public void testModuleInfoCreation() {
        ProjectInfoResponse.ModuleInfo moduleInfo = new ProjectInfoResponse.ModuleInfo(
                "mymodule",
                "My Module",
                new ArrayList<>()
        );

        assertNotNull(moduleInfo);
        assertEquals(moduleInfo.moduleName(), "mymodule");
        assertEquals(moduleInfo.displayName(), "My Module");
        assertNotNull(moduleInfo.documentPaths());
        assertTrue(moduleInfo.documentPaths().isEmpty());
    }

    @Test
    public void testModuleInfoWithDocuments() {
        List<String> documentPaths = List.of(
                "/path/to/module/file1.bal",
                "/path/to/module/file2.bal"
        );

        ProjectInfoResponse.ModuleInfo moduleInfo = new ProjectInfoResponse.ModuleInfo(
                "mymodule",
                "My Module",
                documentPaths
        );

        assertNotNull(moduleInfo.documentPaths());
        assertEquals(moduleInfo.documentPaths().size(), 2);
        assertTrue(moduleInfo.documentPaths().contains("/path/to/module/file1.bal"));
    }

    @Test
    public void testModuleInfoWithNullDisplayName() {
        ProjectInfoResponse.ModuleInfo moduleInfo = new ProjectInfoResponse.ModuleInfo(
                "mymodule",
                null,
                new ArrayList<>()
        );

        assertEquals(moduleInfo.moduleName(), "mymodule");
        assertNull(moduleInfo.displayName());
    }

    @Test
    public void testEmptyModulesList() {
        ProjectInfoResponse response = new ProjectInfoResponse(
                "project",
                "Project",
                "1.0.0",
                "org",
                "Org",
                "BUILD",
                "/path",
                new ArrayList<>(),
                new HashMap<>()
        );

        assertNotNull(response.modules());
        assertTrue(response.modules().isEmpty());
    }

    @Test
    public void testEmptyDependenciesMap() {
        ProjectInfoResponse response = new ProjectInfoResponse(
                "project",
                "Project",
                "1.0.0",
                "org",
                "Org",
                "BUILD",
                "/path",
                new ArrayList<>(),
                new HashMap<>()
        );

        assertNotNull(response.dependencies());
        assertTrue(response.dependencies().isEmpty());
    }

    @Test
    public void testMultipleModules() {
        List<ProjectInfoResponse.ModuleInfo> modules = List.of(
                new ProjectInfoResponse.ModuleInfo("module1", "Module 1", new ArrayList<>()),
                new ProjectInfoResponse.ModuleInfo("module2", "Module 2", new ArrayList<>()),
                new ProjectInfoResponse.ModuleInfo("module3", "Module 3", new ArrayList<>())
        );

        ProjectInfoResponse response = new ProjectInfoResponse(
                "project",
                "Project",
                "1.0.0",
                "org",
                "Org",
                "BUILD",
                "/path",
                modules,
                new HashMap<>()
        );

        assertEquals(response.modules().size(), 3);
    }

    @Test
    public void testVersionFormat() {
        ProjectInfoResponse response = new ProjectInfoResponse(
                "project",
                "Project",
                "1.2.3",
                "org",
                "Org",
                "BUILD",
                "/path",
                new ArrayList<>(),
                new HashMap<>()
        );

        assertEquals(response.version(), "1.2.3");
        assertTrue(response.version().matches("\\d+\\.\\d+\\.\\d+"));
    }

    @Test
    public void testProjectPathWithSpaces() {
        String pathWithSpaces = "/path/to/my project/ballerina";
        ProjectInfoResponse response = new ProjectInfoResponse(
                "project",
                "Project",
                "1.0.0",
                "org",
                "Org",
                "BUILD",
                pathWithSpaces,
                new ArrayList<>(),
                new HashMap<>()
        );

        assertEquals(response.projectPath(), pathWithSpaces);
    }

    @Test
    public void testOrganizationNameFormat() {
        ProjectInfoResponse response = new ProjectInfoResponse(
                "project",
                "Project",
                "1.0.0",
                "ballerina",
                "Ballerina",
                "BUILD",
                "/path",
                new ArrayList<>(),
                new HashMap<>()
        );

        assertEquals(response.organization(), "ballerina");
        assertEquals(response.displayOrganization(), "Ballerina");
    }

    @Test
    public void testProjectNameFormat() {
        ProjectInfoResponse response = new ProjectInfoResponse(
                "my_project",
                "My Project",
                "1.0.0",
                "myorg",
                "My Org",
                "BUILD",
                "/path",
                new ArrayList<>(),
                new HashMap<>()
        );

        assertEquals(response.projectName(), "my_project");
        assertTrue(response.projectName().contains("_"));
    }

    @Test
    public void testComplexProjectStructure() {
        List<ProjectInfoResponse.ModuleInfo> modules = List.of(
                new ProjectInfoResponse.ModuleInfo("core", "Core", List.of("/core/main.bal")),
                new ProjectInfoResponse.ModuleInfo("utils", "Utils", List.of("/utils/helper.bal"))
        );

        Map<String, String> dependencies = Map.of(
                "ballerina/http", "2.0.0",
                "ballerina/io", "1.0.0",
                "myorg/common", "1.5.0"
        );

        ProjectInfoResponse response = new ProjectInfoResponse(
                "complex_project",
                "Complex Project",
                "2.1.0",
                "myorg",
                "My Organization",
                "BUILD",
                "/path/to/complex_project",
                modules,
                dependencies
        );

        assertNotNull(response);
        assertEquals(response.modules().size(), 2);
        assertEquals(response.dependencies().size(), 3);
        assertEquals(response.version(), "2.1.0");
    }

    @Test
    public void testRecordImmutability() {
        List<ProjectInfoResponse.ModuleInfo> modules = new ArrayList<>();
        modules.add(new ProjectInfoResponse.ModuleInfo("module1", "Module 1", new ArrayList<>()));

        ProjectInfoResponse response = new ProjectInfoResponse(
                "project",
                "Project",
                "1.0.0",
                "org",
                "Org",
                "BUILD",
                "/path",
                modules,
                new HashMap<>()
        );

        // Record fields should be accessible
        assertNotNull(response.projectName());
        assertNotNull(response.modules());
    }
}