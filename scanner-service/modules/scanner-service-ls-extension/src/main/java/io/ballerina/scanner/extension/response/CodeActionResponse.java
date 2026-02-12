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

package io.ballerina.scanner.extension.response;

import io.ballerina.scanner.extension.codeaction.ScannerCodeAction;

import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO for the {@code scanner/getCodeActions} endpoint.
 */
public class CodeActionResponse extends BaseResponse {

    private List<ScannerCodeAction> codeActions;

    public CodeActionResponse() {
        this.codeActions = new ArrayList<>();
    }

    public List<ScannerCodeAction> getCodeActions() {
        return new ArrayList<>(this.codeActions);
    }

    public void setCodeActions(List<ScannerCodeAction> codeActions) {
        this.codeActions = (codeActions != null) ? new ArrayList<>(codeActions) : new ArrayList<>();
    }
}
