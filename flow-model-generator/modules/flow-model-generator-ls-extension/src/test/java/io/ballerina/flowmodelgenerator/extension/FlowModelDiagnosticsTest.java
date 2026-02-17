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

package io.ballerina.flowmodelgenerator.extension;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.ballerina.flowmodelgenerator.extension.request.FlowModelSourceGeneratorRequest;
import io.ballerina.modelgenerator.commons.AbstractLSTest;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Test cases for the flow model diagnostics API.
 *
 * @since 1.0.0
 */
public class FlowModelDiagnosticsTest extends AbstractLSTest {

    @Override
    @Test(dataProvider = "data-provider")
    public void test(Path config) throws IOException {
        Path configJsonPath = configDir.resolve(config);
        TestConfig testConfig = gson.fromJson(Files.newBufferedReader(configJsonPath), TestConfig.class);
        String sourcePath = getSourcePath(testConfig.source());

        notifyDidOpen(sourcePath);
        FlowModelSourceGeneratorRequest request =
                new FlowModelSourceGeneratorRequest(sourcePath, testConfig.flowNode());
        JsonElement flowNode = getResponse(request).get("flowNode");
        notifyDidClose(sourcePath);

        assertDiagnosticsIncludeRange(flowNode, "flowNode");
        JsonElement normalizedActualFlowNode = flowNode.deepCopy();
        stripDiagnosticRanges(normalizedActualFlowNode);
        JsonElement normalizedExpectedFlowNode = testConfig.output().deepCopy();
        stripDiagnosticRanges(normalizedExpectedFlowNode);
        if (!normalizedActualFlowNode.equals(normalizedExpectedFlowNode)) {
            TestConfig updateConfig = new TestConfig(testConfig.source(), testConfig.description(),
                    testConfig.flowNode(), flowNode);
//            updateConfig(configJsonPath, updateConfig);
            compareJsonElements(normalizedActualFlowNode, normalizedExpectedFlowNode);
            Assert.fail(String.format("Failed test: '%s' (%s)", testConfig.description(), configJsonPath));
        }
    }

    @Test
    public void testMultipleRequests() throws IOException, InterruptedException {
        // Load the template test config
        Path configJsonPath = configDir.resolve("variable1.json");
        TestConfig testConfig = gson.fromJson(Files.newBufferedReader(configJsonPath), TestConfig.class);
        String sourcePath = getSourcePath(testConfig.source());
        notifyDidOpen(sourcePath);

        // Fire multiple requests with the same flow node to test race conditions
        JsonElement flowNode = testConfig.flowNode();
        for (int i = 0; i < 3; i++) {
            getResponse(new FlowModelSourceGeneratorRequest(sourcePath, flowNode));
            Thread.sleep(50);
        }

        // Make a final request and verify it returns the expected response
        FlowModelSourceGeneratorRequest finalReq = new FlowModelSourceGeneratorRequest(sourcePath, flowNode);
        JsonObject response = getResponse(finalReq);
        notifyDidClose(sourcePath);
        JsonElement actualFlowNode = response.get("flowNode");
        assertDiagnosticsIncludeRange(actualFlowNode, "flowNode");
        JsonElement normalizedActualFlowNode = actualFlowNode.deepCopy();
        stripDiagnosticRanges(normalizedActualFlowNode);
        JsonElement normalizedExpectedFlowNode = testConfig.output().deepCopy();
        stripDiagnosticRanges(normalizedExpectedFlowNode);
        Assert.assertEquals(normalizedActualFlowNode, normalizedExpectedFlowNode);
    }

    @Override
    protected String getResourceDir() {
        return "flow_model_diagnostics";
    }

    @Override
    protected Class<? extends AbstractLSTest> clazz() {
        return FlowModelDiagnosticsTest.class;
    }

    @Override
    protected String getApiName() {
        return "diagnostics";
    }

    private record TestConfig(String source, String description, JsonElement flowNode, JsonElement output) {

        public String description() {
            return description == null ? "" : description;
        }
    }

    private static void stripDiagnosticRanges(JsonElement jsonElement) {
        if (jsonElement == null || jsonElement.isJsonNull()) {
            return;
        }
        if (jsonElement.isJsonArray()) {
            JsonArray jsonArray = jsonElement.getAsJsonArray();
            for (JsonElement element : jsonArray) {
                stripDiagnosticRanges(element);
            }
            return;
        }
        if (!jsonElement.isJsonObject()) {
            return;
        }
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        if (jsonObject.has("diagnostics")) {
            JsonElement diagnosticsElement = jsonObject.get("diagnostics");
            if (diagnosticsElement.isJsonObject()) {
                JsonArray diagnosticsArray = diagnosticsElement.getAsJsonObject().getAsJsonArray("diagnostics");
                if (diagnosticsArray != null) {
                    for (JsonElement diagnosticElement : diagnosticsArray) {
                        if (diagnosticElement.isJsonObject()) {
                            diagnosticElement.getAsJsonObject().remove("range");
                        }
                    }
                }
            }
        }
        for (String key : jsonObject.keySet()) {
            stripDiagnosticRanges(jsonObject.get(key));
        }
    }

    private static void assertDiagnosticsIncludeRange(JsonElement jsonElement, String path) {
        if (jsonElement == null || jsonElement.isJsonNull()) {
            return;
        }
        if (jsonElement.isJsonArray()) {
            JsonArray jsonArray = jsonElement.getAsJsonArray();
            for (int i = 0; i < jsonArray.size(); i++) {
                assertDiagnosticsIncludeRange(jsonArray.get(i), path + "[" + i + "]");
            }
            return;
        }
        if (!jsonElement.isJsonObject()) {
            return;
        }
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        if (jsonObject.has("diagnostics")) {
            JsonElement diagnosticsElement = jsonObject.get("diagnostics");
            if (diagnosticsElement.isJsonObject()) {
                JsonArray diagnosticsArray = diagnosticsElement.getAsJsonObject().getAsJsonArray("diagnostics");
                if (diagnosticsArray != null) {
                    for (int i = 0; i < diagnosticsArray.size(); i++) {
                        JsonElement diagnosticElement = diagnosticsArray.get(i);
                        Assert.assertTrue(diagnosticElement.isJsonObject()
                                        && diagnosticElement.getAsJsonObject().has("range"),
                                String.format("Expected diagnostic range at %s.diagnostics.diagnostics[%d]", path, i));
                    }
                }
            }
        }
        for (String key : jsonObject.keySet()) {
            assertDiagnosticsIncludeRange(jsonObject.get(key), path + "." + key);
        }
    }
}
