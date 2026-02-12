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

import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.compiler.syntax.tree.Token;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Code action handler for rule {@code ballerina:3} — <em>Non-isolated public function</em>.
 * <p>
 * Adds the {@code isolated} keyword to a {@code public function} definition.
 */
public class IsolatedPublicFunctionCodeAction {

    public static final String RULE_ID = "ballerina:3";

    private IsolatedPublicFunctionCodeAction() {
    }

    /**
     * Returns the rule ID handled by this code action.
     */
    public static String getRuleId() {
        return RULE_ID;
    }

    /**
     * Computes quick-fix actions for a non-isolated public function diagnostic using AST.
     *
     * @param syntaxTree   the syntax tree of the document
     * @param startLine    0-based start line of the diagnostic range
     * @param startColumn  0-based start column
     * @param endLine      0-based end line
     * @param endColumn    0-based end column
     * @return list of code actions
     */
    public static List<ScannerCodeAction> getCodeActions(
            SyntaxTree syntaxTree,
            int startLine, int startColumn,
            int endLine, int endColumn) {

        List<FunctionDefinitionNode> functionNodes = new ArrayList<>();
        syntaxTree.rootNode().accept(new NodeVisitor() {
            @Override
            public void visit(FunctionDefinitionNode node) {
                if (CodeActionUtils.isInRange(node, startLine, startColumn, endLine, endColumn)) {
                    functionNodes.add(node);
                }
                visitSyntaxNode(node);
            }
        });

        if (functionNodes.isEmpty()) {
            return Collections.emptyList();
        }

        List<ScannerCodeAction> actions = new ArrayList<>();
        for (FunctionDefinitionNode functionNode : functionNodes) {
            boolean isPublic = false;
            boolean isIsolated = false;
            
            NodeList<Token> qualifiers = functionNode.qualifierList();
            for (Token qualifier : qualifiers) {
                if (qualifier.kind() == SyntaxKind.PUBLIC_KEYWORD) {
                    isPublic = true;
                } else if (qualifier.kind() == SyntaxKind.ISOLATED_KEYWORD) {
                    isIsolated = true;
                }
            }

            if (isPublic && !isIsolated) {
                Token functionKeyword = functionNode.functionKeyword();
                int startLinePosition = functionKeyword.lineRange().startLine().line();
                int startColumnPosition = functionKeyword.lineRange().startLine().offset();
                
                TextEdit edit = new TextEdit(
                        new Range(
                                new Position(startLinePosition, startColumnPosition),
                                new Position(startLinePosition, startColumnPosition)
                        ),
                        "isolated "
                );

                actions.add(new ScannerCodeAction(
                        "Mark as 'isolated'",
                        "quickfix",
                        Collections.singletonList(edit)
                ));
            }
        }

        return actions;
    }

}
