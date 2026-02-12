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

import io.ballerina.compiler.syntax.tree.AssignmentStatementNode;
import io.ballerina.compiler.syntax.tree.BindingPatternNode;
import io.ballerina.compiler.syntax.tree.CaptureBindingPatternNode;
import io.ballerina.compiler.syntax.tree.CheckExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.FunctionSignatureNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.compiler.syntax.tree.QualifiedNameReferenceNode;
import io.ballerina.compiler.syntax.tree.ReturnTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.compiler.syntax.tree.VariableDeclarationNode;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;

import java.util.ArrayList;
import java.util.List;

/**
 * Code action handler for rule {@code ballerina:1} — <em>Avoid checkpanic</em>.
 * <p>
 * Replaces every occurrence of the keyword {@code checkpanic} within the
 * diagnostic range with the safer {@code check} keyword, and ensures the
 * enclosing function's return type includes {@code error}.
 */
public class CheckpanicCodeAction {

    public static final String RULE_ID = "ballerina:1";
    private static final String CHECKPANIC = "checkpanic";
    private static final String CHECK = "check";

    private CheckpanicCodeAction() {
    }

    /**
     * Returns the rule ID handled by this code action.
     */
    public static String getRuleId() {
        return RULE_ID;
    }

    /**
     * Computes quick-fix actions for a {@code checkpanic} diagnostic using AST.
     *
     * @param syntaxTree   the syntax tree of the document
     * @param startLine    0-based start line of the diagnostic range
     * @param startColumn  0-based start column
     * @param endLine      0-based end line
     * @param endColumn    0-based end column
     * @return list of code actions (may be empty if no {@code checkpanic} found)
     */
    public static List<ScannerCodeAction> getCodeActions(
            SyntaxTree syntaxTree,
            int startLine, int startColumn,
            int endLine, int endColumn) {

        // Collect all CheckExpressionNodes with 'checkpanic' keyword in the range
        List<CheckExpressionNode> checkpanicNodes = new ArrayList<>();
        syntaxTree.rootNode().accept(new NodeVisitor() {
            @Override
            public void visit(CheckExpressionNode node) {
                if (CHECKPANIC.equals(node.checkKeyword().text())
                        && CodeActionUtils.isInRange(node, startLine, startColumn, endLine, endColumn)) {
                    checkpanicNodes.add(node);
                }
                // Continue visiting child nodes
                visitSyntaxNode(node);
            }
        });

        List<ScannerCodeAction> actions = new ArrayList<>();
        for (CheckExpressionNode checkpanicNode : checkpanicNodes) {
            List<TextEdit> edits = new ArrayList<>();

            // Edit 1: Replace 'checkpanic' keyword with 'check'
            Token keyword = checkpanicNode.checkKeyword();
            int kwStartLine = keyword.lineRange().startLine().line();
            int kwStartCol = keyword.lineRange().startLine().offset();
            int kwEndLine = keyword.lineRange().endLine().line();
            int kwEndCol = keyword.lineRange().endLine().offset();

            edits.add(new TextEdit(
                    new Range(
                            new Position(kwStartLine, kwStartCol),
                            new Position(kwEndLine, kwEndCol)
                    ),
                    CHECK
            ));

            // Edit 2: Fix enclosing function's return type if needed
            TextEdit returnTypeEdit = buildReturnTypeEdit(checkpanicNode);
            if (returnTypeEdit != null) {
                edits.add(returnTypeEdit);
            }

            actions.add(new ScannerCodeAction(
                    "Replace 'checkpanic' with 'check'",
                    "quickfix",
                    edits
            ));

            // Extract Type-Guard: only if checkpanic is the direct RHS of an assignment/declaration
            ScannerCodeAction typeGuardAction = buildTypeGuardAction(checkpanicNode);
            if (typeGuardAction != null) {
                actions.add(typeGuardAction);
            }
        }

        return actions;
    }

    /**
     * Builds a code action that extracts the checkpanic result into a type-guard
     * (e.g. `if result is error { }`).
     * Supports VariableDeclarationNode and AssignmentStatementNode.
     */
    private static ScannerCodeAction buildTypeGuardAction(CheckExpressionNode checkNode) {
        Node parent = checkNode.parent();
        String varName = null;
        Node statementNode = null;
        TextEdit returnTypeEdit = null;

        if (parent.kind() == SyntaxKind.LOCAL_VAR_DECL || parent.kind() == SyntaxKind.MODULE_VAR_DECL) {
            VariableDeclarationNode varDeclNode = (VariableDeclarationNode) parent;
            if (varDeclNode.initializer().isPresent() && varDeclNode.initializer().get() == checkNode) {
                statementNode = varDeclNode;
                BindingPatternNode bindingPattern = varDeclNode.typedBindingPattern().bindingPattern();
                if (bindingPattern.kind() == SyntaxKind.CAPTURE_BINDING_PATTERN) {
                    varName = ((CaptureBindingPatternNode) bindingPattern).variableName().text();
                }

                // If type is not `var` and doesn't contain error, we must append `|error`
                String typeStr = varDeclNode.typedBindingPattern().typeDescriptor().toSourceCode().trim();
                if (!typeStr.equals("var") && !typeStr.contains("error")) {
                    int rtStartLine = varDeclNode.typedBindingPattern().typeDescriptor()
                                                                    .lineRange().startLine().line();
                    int rtStartCol = varDeclNode.typedBindingPattern().typeDescriptor()
                                                                    .lineRange().startLine().offset();
                    int rtEndLine = varDeclNode.typedBindingPattern().typeDescriptor()
                                                                    .lineRange().endLine().line();
                    int rtEndCol = varDeclNode.typedBindingPattern().typeDescriptor()
                                                                    .lineRange().endLine().offset();

                    returnTypeEdit = new TextEdit(
                            new Range(
                                    new Position(rtStartLine, rtStartCol),
                                    new Position(rtEndLine, rtEndCol)
                            ),
                            typeStr + "|error"
                    );
                }
            }
        } else if (parent.kind() == SyntaxKind.ASSIGNMENT_STATEMENT) {
            AssignmentStatementNode assignNode = (AssignmentStatementNode) parent;
            if (assignNode.expression() == checkNode) {
                statementNode = assignNode;
                Node varRef = assignNode.varRef();
                if (varRef.kind() == SyntaxKind.SIMPLE_NAME_REFERENCE) {
                    varName = ((SimpleNameReferenceNode) varRef).name().text();
                } else if (varRef.kind() == SyntaxKind.QUALIFIED_NAME_REFERENCE) {
                    varName = ((QualifiedNameReferenceNode) varRef).identifier().text();
                }
            }
        }

        if (varName == null || statementNode == null) {
            return null;
        }

        List<TextEdit> edits = new ArrayList<>();

        if (returnTypeEdit != null) {
            edits.add(returnTypeEdit);
        }

        // Edit 1: Delete `checkpanic ` keyword
        int kwStartLine = checkNode.checkKeyword().lineRange().startLine().line();
        int kwStartCol = checkNode.checkKeyword().lineRange().startLine().offset();
        int exprStartLine = checkNode.expression().lineRange().startLine().line();
        int exprStartCol = checkNode.expression().lineRange().startLine().offset();
        edits.add(new TextEdit(
                new Range(
                        new Position(kwStartLine, kwStartCol),
                        new Position(exprStartLine, exprStartCol)
                ),
                ""
        ));

        // Edit 2: Insert the type guard block after the statement
        int stmtEndLine = statementNode.lineRange().endLine().line();
        int stmtEndCol = statementNode.lineRange().endLine().offset();
        
        // Find leading spaces of the statement for indentation
        int stmtStartCol = statementNode.lineRange().startLine().offset();
        String indent = " ".repeat(Math.max(0, stmtStartCol));

        String typeGuardSnippet = System.lineSeparator() + System.lineSeparator() + indent +
                "if " + varName + " is error {" + System.lineSeparator() +
                indent + "    // handle error" + System.lineSeparator() +
                indent + "}";

        edits.add(new TextEdit(
                new Range(
                        new Position(stmtEndLine, stmtEndCol),
                        new Position(stmtEndLine, stmtEndCol)
                ),
                typeGuardSnippet
        ));

        return new ScannerCodeAction(
                "Extract to type guard",
                "quickfix",
                edits
        );
    }

    /**
     * Walks up the AST from the given node to find the enclosing
     * {@link FunctionDefinitionNode} and produces a {@link TextEdit} to
     * ensure its return type includes {@code error}.
     */
    private static TextEdit buildReturnTypeEdit(Node node) {
        FunctionDefinitionNode funcNode = findEnclosingFunction(node);
        if (funcNode == null) {
            return null;
        }

        FunctionSignatureNode signature = funcNode.functionSignature();

        if (signature.returnTypeDesc().isPresent()) {
            // Function already has a return type — check if it includes 'error'
            ReturnTypeDescriptorNode returnTypeDesc = signature.returnTypeDesc().get();
            String returnTypeText = returnTypeDesc.type().toSourceCode().trim();

            if (returnTypeText.contains("error")) {
                // Already has error in return type — no edit needed
                return null;
            }

            // Append |error to the existing return type
            int rtStartLine = returnTypeDesc.type().lineRange().startLine().line();
            int rtStartCol = returnTypeDesc.type().lineRange().startLine().offset();
            int rtEndLine = returnTypeDesc.type().lineRange().endLine().line();
            int rtEndCol = returnTypeDesc.type().lineRange().endLine().offset();

            return new TextEdit(
                    new Range(
                            new Position(rtStartLine, rtStartCol),
                            new Position(rtEndLine, rtEndCol)
                    ),
                    returnTypeText + "|error"
            );
        } else {
            // No return type — insert 'returns error? ' before the opening '{'
            Token closeParen = signature.closeParenToken();
            int cpEndLine = closeParen.lineRange().endLine().line();
            int cpEndCol = closeParen.lineRange().endLine().offset();

            return new TextEdit(
                    new Range(
                            new Position(cpEndLine, cpEndCol),
                            new Position(cpEndLine, cpEndCol)
                    ),
                    " returns error?"
            );
        }
    }

    /**
     * Walks up the parent chain to find the enclosing {@link FunctionDefinitionNode}.
     */
    private static FunctionDefinitionNode findEnclosingFunction(Node node) {
        Node current = node.parent();
        while (current != null) {
            if (current.kind() == SyntaxKind.FUNCTION_DEFINITION
                    || current.kind() == SyntaxKind.OBJECT_METHOD_DEFINITION
                    || current.kind() == SyntaxKind.RESOURCE_ACCESSOR_DEFINITION) {
                return (FunctionDefinitionNode) current;
            }
            current = current.parent();
        }
        return null;
    }

}
