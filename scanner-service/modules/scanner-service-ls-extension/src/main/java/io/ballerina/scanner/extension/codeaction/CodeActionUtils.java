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

import io.ballerina.compiler.syntax.tree.Node;

/**
 * Utility methods for Scanner Code Actions.
 */
public class CodeActionUtils {

    private CodeActionUtils() {
    }

    /**
     * Checks whether a node's location overlaps with the given diagnostic range.
     */
    public static boolean isInRange(Node node,
                                    int startLine, int startColumn,
                                    int endLine, int endColumn) {
        int nodeStartLine = node.lineRange().startLine().line();
        int nodeStartCol = node.lineRange().startLine().offset();
        int nodeEndLine = node.lineRange().endLine().line();
        int nodeEndCol = node.lineRange().endLine().offset();

        // Check if the node's range overlaps with the diagnostic range
        if (nodeEndLine < startLine || (nodeEndLine == startLine && nodeEndCol <= startColumn)) {
            return false;
        }
        if (nodeStartLine > endLine || (nodeStartLine == endLine && nodeStartCol >= endColumn)) {
            return false;
        }
        return true;
    }
}
