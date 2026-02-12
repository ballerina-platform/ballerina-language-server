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

import io.ballerina.compiler.syntax.tree.DefaultableParameterNode;
import io.ballerina.compiler.syntax.tree.FunctionSignatureNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.compiler.syntax.tree.ParameterNode;
import io.ballerina.compiler.syntax.tree.RequiredParameterNode;
import io.ballerina.compiler.syntax.tree.RestParameterNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;

import java.util.ArrayList;
import java.util.List;

/**
 * Code action handler for rule {@code ballerina:2} — <em>Unused function parameter</em>.
 * <p>
 * Removes the unused parameter from the function signature. Note that this
 * is a signature-only change; it does not update call sites across the project.
 */
public class UnusedParamCodeAction {

    public static final String RULE_ID = "ballerina:2";

    private UnusedParamCodeAction() {
    }

    /**
     * Returns the rule ID handled by this code action.
     */
    public static String getRuleId() {
        return RULE_ID;
    }

    /**
     * Computes quick-fix actions for an unused parameter diagnostic using AST.
     */
    public static List<ScannerCodeAction> getCodeActions(
            SyntaxTree syntaxTree,
            int startLine, int startColumn,
            int endLine, int endColumn) {

        List<Node> paramNodes = new ArrayList<>();
        syntaxTree.rootNode().accept(new NodeVisitor() {
            @Override
            public void visit(RequiredParameterNode node) {
                if (CodeActionUtils.isInRange(node, startLine, startColumn, endLine, endColumn)) {
                    paramNodes.add(node);
                }
                visitSyntaxNode(node);
            }

            @Override
            public void visit(DefaultableParameterNode node) {
                if (CodeActionUtils.isInRange(node, startLine, startColumn, endLine, endColumn)) {
                    paramNodes.add(node);
                }
                visitSyntaxNode(node);
            }

            @Override
            public void visit(RestParameterNode node) {
                if (CodeActionUtils.isInRange(node, startLine, startColumn, endLine, endColumn)) {
                    paramNodes.add(node);
                }
                visitSyntaxNode(node);
            }
        });

        if (paramNodes.isEmpty()) {
            return new ArrayList<>();
        }

        Node paramNode = paramNodes.get(0);
        
        // Ensure this parameter belongs to a FunctionSignatureNode
        if (paramNode.parent() == null || paramNode.parent().kind() != SyntaxKind.FUNCTION_SIGNATURE) {
            return new ArrayList<>();
        }

        FunctionSignatureNode signatureNode = (FunctionSignatureNode) paramNode.parent();
        io.ballerina.compiler.syntax.tree.SeparatedNodeList<ParameterNode> params = signatureNode.parameters();
        
        int paramCount = params.size();
        if (paramCount == 0) {
            return new ArrayList<>();
        }

        // Determine if we need to delete an adjacent comma
        int paramIndex = -1;
        for (int i = 0; i < paramCount; i++) {
            if (params.get(i) == paramNode) {
                paramIndex = i;
                break;
            }
        }

        if (paramIndex == -1) {
            return new ArrayList<>();
        }

        int deleteStartLine = paramNode.lineRange().startLine().line();
        int deleteStartCol = paramNode.lineRange().startLine().offset();
        int deleteEndLine = paramNode.lineRange().endLine().line();
        int deleteEndCol = paramNode.lineRange().endLine().offset();

        // Handle commas
        if (paramCount > 1) {
            if (paramIndex == paramCount - 1) {
                // Last parameter -> delete the preceding comma and spaces
                int prevCommaEndLine = params.getSeparator(paramIndex - 1).lineRange().startLine().line();
                int prevCommaEndCol = params.getSeparator(paramIndex - 1).lineRange().startLine().offset();
                deleteStartLine = prevCommaEndLine;
                deleteStartCol = prevCommaEndCol;
            } else {
                // Not the last parameter -> delete the succeeding comma and spaces
                int nextParamStartLine = params.get(paramIndex + 1).lineRange().startLine().line();
                int nextParamStartCol = params.get(paramIndex + 1).lineRange().startLine().offset();
                deleteEndLine = nextParamStartLine;
                deleteEndCol = nextParamStartCol;
            }
        }

        List<TextEdit> edits = new ArrayList<>();
        edits.add(new TextEdit(
                new Range(
                        new Position(deleteStartLine, deleteStartCol),
                        new Position(deleteEndLine, deleteEndCol)
                ),
                ""
        ));

        List<ScannerCodeAction> actions = new ArrayList<>();
        actions.add(new ScannerCodeAction(
                "Remove unused parameter",
                "quickfix",
                edits
        ));

        return actions;
    }

}
