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

import io.ballerina.compiler.syntax.tree.SyntaxTree;

import java.util.Collections;
import java.util.List;

/**
 * Central registry that dispatches code-action requests to the
 * appropriate rule-specific handler.
 * <p>
 * New rules should be added to the {@link #getCodeActions} method
 * as additional {@code case} branches.
 */
public class ScannerCodeActionProvider {

    /**
     * Returns the list of code actions available for the given rule.
     *
     * @param ruleId      the scanner rule identifier (e.g. "ballerina:1")
     * @param syntaxTree  the syntax tree of the document
     * @param startLine   0-based start line of the diagnostic
     * @param startColumn 0-based start column
     * @param endLine     0-based end line
     * @param endColumn   0-based end column
     * @return list of code actions, or empty list if none available
     */
    public List<ScannerCodeAction> getCodeActions(
            String ruleId,
            SyntaxTree syntaxTree,
            int startLine, int startColumn,
            int endLine, int endColumn) {

        if (ruleId == null) {
            return Collections.emptyList();
        }

        switch (ruleId) {
            case CheckpanicCodeAction.RULE_ID:
                return CheckpanicCodeAction.getCodeActions(
                        syntaxTree, startLine, startColumn, endLine, endColumn);
            case UnusedParamCodeAction.RULE_ID:
                return UnusedParamCodeAction.getCodeActions(
                        syntaxTree, startLine, startColumn, endLine, endColumn);
            case IsolatedPublicFunctionCodeAction.RULE_ID:
                return IsolatedPublicFunctionCodeAction.getCodeActions(
                        syntaxTree, startLine, startColumn, endLine, endColumn);
            default:
                return Collections.emptyList();
        }
    }
}
