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

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Path;

public class IsolatedPublicFunctionCodeActionTest extends AbstractScannerCodeActionTest {

    @Test(dataProvider = "data-provider")
    public void test(String configName, String sourceName) throws IOException {
        runTest(configName, sourceName);
    }

    @Override
    protected Path getResourceDir() {
        return Path.of("src", "test", "resources", "codeaction", "isolatedpublicfunction");
    }

    @Override
    protected String getRuleId() {
        return IsolatedPublicFunctionCodeAction.RULE_ID;
    }

    @DataProvider(name = "data-provider")
    @Override
    public Object[][] dataProvider() {
        return new Object[][]{
                {"add_isolated_qualifier.json", "add_isolated_qualifier.bal"}
        };
    }
}
