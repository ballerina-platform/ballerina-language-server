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

import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.tools.text.LineRange;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.*;

/**
 * Unit tests for {@link Codedata}.
 *
 * @since 1.0.0
 */
public class CodedataTest {

    @Test(description = "Test Codedata record creation")
    public void testCodedataCreation() {
        // Arrange & Act
        Codedata codedata = new Codedata(
                NodeKind.FUNCTION,
                "ballerina",
                "http",
                "http",
                "Client",
                "get",
                "2.0.0",
                null,
                "client->get(\"/path\")",
                "httpClient",
                "/path",
                1,
                true,
                false,
                "json",
                new HashMap<>()
        );

        // Assert
        assertEquals(codedata.node(), NodeKind.FUNCTION);
        assertEquals(codedata.org(), "ballerina");
        assertEquals(codedata.module(), "http");
        assertEquals(codedata.packageName(), "http");
        assertEquals(codedata.object(), "Client");
        assertEquals(codedata.symbol(), "get");
        assertEquals(codedata.version(), "2.0.0");
        assertEquals(codedata.sourceCode(), "client->get(\"/path\")");
        assertEquals(codedata.parentSymbol(), "httpClient");
        assertEquals(codedata.resourcePath(), "/path");
        assertEquals(codedata.id(), Integer.valueOf(1));
        assertTrue(codedata.isNew());
        assertFalse(codedata.isGenerated());
        assertEquals(codedata.inferredReturnType(), "json");
    }

    @Test(description = "Test Codedata toString method")
    public void testCodedataToString() {
        // Arrange
        Codedata codedata = new Codedata(
                NodeKind.FUNCTION_CALL,
                "ballerina",
                "http",
                "http",
                "Client",
                "get",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        // Act
        String result = codedata.toString();

        // Assert
        assertEquals(result, "FUNCTION_CALL:ballerina:http:Client:get");
    }

    @Test(description = "Test Codedata getImportSignature")
    public void testGetImportSignature() {
        // Arrange
        Codedata codedata = new Codedata(
                NodeKind.FUNCTION,
                "ballerinax",
                "aws.s3",
                "aws.s3",
                null,
                null,
                "1.0.0",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        // Act
        String importSignature = codedata.getImportSignature();

        // Assert
        assertEquals(importSignature, "ballerinax/aws.s3");
    }

    @Test(description = "Test Codedata getModuleId")
    public void testGetModuleId() {
        // Arrange
        Codedata codedata = new Codedata(
                NodeKind.FUNCTION,
                "ballerina",
                "http",
                "http",
                null,
                null,
                "2.5.0",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        // Act
        String moduleId = codedata.getModuleId();

        // Assert
        assertEquals(moduleId, "ballerina/http:2.5.0");
    }

    @Test(description = "Test Codedata getModulePrefix")
    public void testGetModulePrefix() {
        // Arrange
        Codedata codedata = new Codedata(
                NodeKind.FUNCTION,
                "ballerina",
                "lang.array",
                "lang.array",
                null,
                null,
                "1.0.0",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        // Act
        String modulePrefix = codedata.getModulePrefix();

        // Assert
        assertEquals(modulePrefix, "array");
    }

    @Test(description = "Test Codedata Builder")
    public void testCodedataBuilder() {
        // Arrange
        Codedata.Builder<Object> builder = new Codedata.Builder<>(null);

        // Act
        Codedata codedata = builder
                .node(NodeKind.REMOTE_ACTION_CALL)
                .org("ballerina")
                .module("sql")
                .packageName("sql")
                .object("Client")
                .symbol("query")
                .version("1.5.0")
                .sourceCode("sqlClient->query(`SELECT * FROM users`)")
                .parentSymbol("sqlClient")
                .id(10)
                .isNew(true)
                .isGenerated(false)
                .inferredReturnType("stream<record {}, error?>")
                .build();

        // Assert
        assertEquals(codedata.node(), NodeKind.REMOTE_ACTION_CALL);
        assertEquals(codedata.org(), "ballerina");
        assertEquals(codedata.module(), "sql");
        assertEquals(codedata.object(), "Client");
        assertEquals(codedata.symbol(), "query");
        assertEquals(codedata.version(), "1.5.0");
        assertEquals(codedata.id(), Integer.valueOf(10));
        assertTrue(codedata.isNew());
        assertFalse(codedata.isGenerated());
    }

    @Test(description = "Test Codedata Builder with node info")
    public void testCodedataBuilderWithNodeInfo() {
        // Arrange
        Node node = mock(Node.class);
        LineRange lineRange = mock(LineRange.class);
        when(node.lineRange()).thenReturn(lineRange);
        when(node.toSourceCode()).thenReturn("  http:Client httpClient = new();  ");

        Codedata.Builder<Object> builder = new Codedata.Builder<>(null);

        // Act
        Codedata codedata = builder
                .node(NodeKind.NEW_CONNECTION)
                .nodeInfo(node)
                .build();

        // Assert
        assertEquals(codedata.lineRange(), lineRange);
        assertEquals(codedata.sourceCode(), "http:Client httpClient = new();");
    }

    @Test(description = "Test Codedata Builder with data")
    public void testCodedataBuilderWithData() {
        // Arrange
        Map<String, Object> data = new HashMap<>();
        data.put("key1", "value1");
        data.put("key2", 42);

        Codedata.Builder<Object> builder = new Codedata.Builder<>(null);

        // Act
        Codedata codedata = builder
                .node(NodeKind.FUNCTION)
                .data("key3", "value3")
                .data(data)
                .build();

        // Assert
        assertNotNull(codedata.data());
        assertEquals(codedata.data().get("key1"), "value1");
        assertEquals(codedata.data().get("key2"), 42);
    }

    @Test(description = "Test Codedata Builder addData method")
    public void testCodedataBuilderAddData() {
        // Arrange
        Codedata.Builder<Object> builder = new Codedata.Builder<>(null);

        // Act
        Codedata codedata = builder
                .node(NodeKind.FUNCTION)
                .addData("metadata", "test")
                .addData("count", 5)
                .build();

        // Assert
        assertNotNull(codedata.data());
        assertEquals(codedata.data().get("metadata"), "test");
        assertEquals(codedata.data().get("count"), 5);
    }

    @Test(description = "Test Codedata Builder from method")
    public void testCodedataBuilderFrom() {
        // Arrange
        Map<String, Object> data = new HashMap<>();
        data.put("test", "value");

        Codedata source = new Codedata(
                NodeKind.METHOD_CALL,
                "ballerina",
                "io",
                "io",
                "Reader",
                "read",
                "1.0.0",
                null,
                "reader.read()",
                "reader",
                null,
                5,
                false,
                true,
                "string",
                data
        );

        Codedata.Builder<Object> builder = new Codedata.Builder<>(null);

        // Act
        Codedata codedata = builder
                .from(source)
                .build();

        // Assert
        assertEquals(codedata.node(), source.node());
        assertEquals(codedata.org(), source.org());
        assertEquals(codedata.module(), source.module());
        assertEquals(codedata.object(), source.object());
        assertEquals(codedata.symbol(), source.symbol());
        assertEquals(codedata.version(), source.version());
        assertEquals(codedata.sourceCode(), source.sourceCode());
        assertEquals(codedata.parentSymbol(), source.parentSymbol());
        assertEquals(codedata.id(), source.id());
        assertEquals(codedata.isNew(), source.isNew());
        assertEquals(codedata.isGenerated(), source.isGenerated());
        assertEquals(codedata.inferredReturnType(), source.inferredReturnType());
        assertEquals(codedata.data(), source.data());
    }

    @Test(description = "Test Codedata with null fields")
    public void testCodedataWithNullFields() {
        // Arrange & Act
        Codedata codedata = new Codedata(
                NodeKind.VARIABLE,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        // Assert
        assertEquals(codedata.toString(), "VARIABLE");
        assertNull(codedata.org());
        assertNull(codedata.module());
        assertNull(codedata.object());
        assertNull(codedata.symbol());
    }
}