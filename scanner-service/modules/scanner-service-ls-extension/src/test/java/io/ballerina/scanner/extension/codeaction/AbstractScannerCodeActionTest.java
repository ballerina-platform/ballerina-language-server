/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com)
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.scanner.extension.codeaction;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextDocuments;
import org.eclipse.lsp4j.TextEdit;
import org.testng.Assert;
import org.testng.annotations.DataProvider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Abstract class for testing Scanner Code Actions using JSON configs.
 */
public abstract class AbstractScannerCodeActionTest {

    private static final Gson GSON = new Gson();

        protected void runTest(String configName, String sourceName) throws IOException {
        Path configPath = getResourceDir().resolve("config").resolve(configName);
        Path sourcePath = getResourceDir().resolve("source").resolve(sourceName);
        performTest(configPath, sourcePath, getRuleId());
    }

    protected abstract Path getResourceDir();

    protected abstract String getRuleId();

    @DataProvider(name = "data-provider")
    public abstract Object[][] dataProvider();

    private void performTest(Path configPath, Path sourcePath, String ruleId) throws IOException {
        String configJson = Files.readString(configPath);
        JsonObject config = GSON.fromJson(configJson, JsonObject.class);

        JsonObject rangeJson = config.getAsJsonObject("range");
        JsonObject start = rangeJson.getAsJsonObject("start");
        JsonObject end = rangeJson.getAsJsonObject("end");

        int startLine = start.get("line").getAsInt();
        int startColumn = start.get("character").getAsInt();
        int endLine = end.get("line").getAsInt();
        int endColumn = end.get("character").getAsInt();

        String sourceCode = Files.readString(sourcePath);
        TextDocument textDocument = TextDocuments.from(sourceCode);
        SyntaxTree syntaxTree = SyntaxTree.from(textDocument);

        ScannerCodeActionProvider provider = new ScannerCodeActionProvider();
        List<ScannerCodeAction> actualActions = provider.getCodeActions(
                ruleId, syntaxTree, startLine, startColumn, endLine, endColumn);

        JsonArray expectedActions = config.getAsJsonArray("expectedActions");
        Assert.assertEquals(actualActions.size(), expectedActions.size(), "Number of code actions mismatch.");

        for (int i = 0; i < expectedActions.size(); i++) {
            JsonObject expectedAction = expectedActions.get(i).getAsJsonObject();
            ScannerCodeAction actualAction = actualActions.get(i);

            Assert.assertEquals(actualAction.getTitle(), expectedAction.get("title").getAsString(), "Title mismatch.");
            Assert.assertEquals(actualAction.getKind(), expectedAction.get("kind").getAsString(), "Kind mismatch.");

            JsonArray expectedEdits = expectedAction.getAsJsonArray("edits");
            Assert.assertEquals(actualAction.getEdits().size(), expectedEdits.size(), "Number of edits mismatch.");

            for (int j = 0; j < expectedEdits.size(); j++) {
                JsonObject expectedEdit = expectedEdits.get(j).getAsJsonObject();
                TextEdit actualEdit = actualAction.getEdits().get(j);

                String expectedNewText = expectedEdit.get("newText").getAsString()
                        .replace("\r\n", "\n")
                        .replace("\\n", "\n");
                String actualNewText = actualEdit.getNewText()
                        .replace("\r\n", "\n");

                Assert.assertEquals(actualNewText, expectedNewText, "New text mismatch.");

                JsonObject editRange = expectedEdit.getAsJsonObject("range");
                JsonObject editStart = editRange.getAsJsonObject("start");
                JsonObject editEnd = editRange.getAsJsonObject("end");

                Assert.assertEquals(actualEdit.getRange().getStart().getLine(),
                        editStart.get("line").getAsInt(), "Start line mismatch.");
                Assert.assertEquals(actualEdit.getRange().getStart().getCharacter(),
                        editStart.get("character").getAsInt(), "Start character mismatch.");
                Assert.assertEquals(actualEdit.getRange().getEnd().getLine(),
                        editEnd.get("line").getAsInt(), "End line mismatch.");
                Assert.assertEquals(actualEdit.getRange().getEnd().getCharacter(),
                        editEnd.get("character").getAsInt(), "End character mismatch.");
            }
        }
    }
}
