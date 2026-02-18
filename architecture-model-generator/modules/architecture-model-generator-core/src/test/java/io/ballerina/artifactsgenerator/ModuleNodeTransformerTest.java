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

package io.ballerina.artifactsgenerator;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.ClassSymbol;
import io.ballerina.compiler.api.symbols.FunctionSymbol;
import io.ballerina.compiler.api.symbols.Qualifier;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.VariableSymbol;
import io.ballerina.compiler.syntax.tree.ClassDefinitionNode;
import io.ballerina.compiler.syntax.tree.EnumDeclarationNode;
import io.ballerina.compiler.syntax.tree.ExpressionFunctionBodyNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.IdentifierToken;
import io.ballerina.compiler.syntax.tree.ListenerDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModuleVariableDeclarationNode;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;
import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.TypedBindingPatternNode;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * Unit tests for {@link ModuleNodeTransformer}.
 *
 * @since 1.0.0
 */
public class ModuleNodeTransformerTest {

    private SemanticModel semanticModel;
    private ModuleNodeTransformer transformer;
    private static final String PROJECT_PATH = "/test/project";

    @BeforeMethod
    public void setUp() {
        semanticModel = mock(SemanticModel.class);
        transformer = new ModuleNodeTransformer(PROJECT_PATH, semanticModel);
    }

    @Test(description = "Test transform main function")
    public void testTransformMainFunction() {
        // Arrange
        FunctionDefinitionNode functionNode = mock(FunctionDefinitionNode.class);
        IdentifierToken functionName = mock(IdentifierToken.class);
        when(functionName.text()).thenReturn("main");
        when(functionNode.functionName()).thenReturn(functionName);
        when(functionNode.functionBody()).thenReturn(mock(io.ballerina.compiler.syntax.tree.FunctionBodyNode.class));
        when(functionNode.kind()).thenReturn(SyntaxKind.FUNCTION_DEFINITION);
        when(functionNode.qualifierList()).thenReturn(NodeList.from());

        // Act
        Optional<Artifact> result = transformer.transform(functionNode);

        // Assert
        assertTrue(result.isPresent());
        Artifact artifact = result.get();
        assertEquals(artifact.name(), "automation");
        assertEquals(artifact.type(), Artifact.Type.AUTOMATION);
    }

    @Test(description = "Test transform resource function")
    public void testTransformResourceFunction() {
        // Arrange
        FunctionDefinitionNode functionNode = mock(FunctionDefinitionNode.class);
        IdentifierToken functionName = mock(IdentifierToken.class);
        when(functionName.text()).thenReturn("get");
        when(functionNode.functionName()).thenReturn(functionName);
        when(functionNode.functionBody()).thenReturn(mock(io.ballerina.compiler.syntax.tree.FunctionBodyNode.class));
        when(functionNode.kind()).thenReturn(SyntaxKind.RESOURCE_ACCESSOR_DEFINITION);
        when(functionNode.relativeResourcePath()).thenReturn(NodeList.from());
        when(functionNode.qualifierList()).thenReturn(NodeList.from());

        // Act
        Optional<Artifact> result = transformer.transform(functionNode);

        // Assert
        assertTrue(result.isPresent());
        Artifact artifact = result.get();
        assertEquals(artifact.type(), Artifact.Type.RESOURCE);
        assertEquals(artifact.accessor(), "get");
    }

    @Test(description = "Test transform remote function")
    public void testTransformRemoteFunction() {
        // Arrange
        FunctionDefinitionNode functionNode = mock(FunctionDefinitionNode.class);
        IdentifierToken functionName = mock(IdentifierToken.class);
        when(functionName.text()).thenReturn("remoteAction");
        when(functionNode.functionName()).thenReturn(functionName);
        when(functionNode.functionBody()).thenReturn(mock(io.ballerina.compiler.syntax.tree.FunctionBodyNode.class));
        when(functionNode.kind()).thenReturn(SyntaxKind.FUNCTION_DEFINITION);

        Token remoteToken = mock(Token.class);
        when(remoteToken.kind()).thenReturn(SyntaxKind.REMOTE_KEYWORD);
        NodeList<Token> qualifiers = NodeList.from(remoteToken);
        when(functionNode.qualifierList()).thenReturn(qualifiers);

        // Act
        Optional<Artifact> result = transformer.transform(functionNode);

        // Assert
        assertTrue(result.isPresent());
        Artifact artifact = result.get();
        assertEquals(artifact.type(), Artifact.Type.REMOTE);
        assertEquals(artifact.name(), "remoteAction");
    }

    @Test(description = "Test transform external function returns empty")
    public void testTransformExternalFunction() {
        // Arrange
        FunctionDefinitionNode functionNode = mock(FunctionDefinitionNode.class);
        when(functionNode.functionBody()).thenReturn(mock(io.ballerina.compiler.syntax.tree.FunctionBodyNode.class));
        when(functionNode.functionBody().kind()).thenReturn(SyntaxKind.EXTERNAL_FUNCTION_BODY);

        // Act
        Optional<Artifact> result = transformer.transform(functionNode);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test(description = "Test transform service declaration")
    public void testTransformServiceDeclaration() {
        // Arrange
        ServiceDeclarationNode serviceNode = mock(ServiceDeclarationNode.class);
        when(serviceNode.expressions()).thenReturn(SeparatedNodeList.from());
        when(serviceNode.typeDescriptor()).thenReturn(Optional.empty());
        when(serviceNode.absoluteResourcePath()).thenReturn(NodeList.from());
        when(serviceNode.members()).thenReturn(NodeList.from());

        // Act
        Optional<Artifact> result = transformer.transform(serviceNode);

        // Assert
        assertTrue(result.isPresent());
        Artifact artifact = result.get();
        assertEquals(artifact.type(), Artifact.Type.SERVICE);
    }

    @Test(description = "Test transform listener declaration")
    public void testTransformListenerDeclaration() {
        // Arrange
        ListenerDeclarationNode listenerNode = mock(ListenerDeclarationNode.class);
        IdentifierToken variableName = mock(IdentifierToken.class);
        when(variableName.text()).thenReturn("httpListener");
        when(listenerNode.variableName()).thenReturn(variableName);
        when(listenerNode.typeDescriptor()).thenReturn(Optional.empty());

        // Act
        Optional<Artifact> result = transformer.transform(listenerNode);

        // Assert
        assertTrue(result.isPresent());
        Artifact artifact = result.get();
        assertEquals(artifact.type(), Artifact.Type.LISTENER);
        assertEquals(artifact.name(), "httpListener");
    }

    @Test(description = "Test transform configurable variable")
    public void testTransformConfigurableVariable() {
        // Arrange
        ModuleVariableDeclarationNode variableNode = mock(ModuleVariableDeclarationNode.class);
        TypedBindingPatternNode bindingPattern = mock(TypedBindingPatternNode.class);
        when(variableNode.typedBindingPattern()).thenReturn(bindingPattern);

        Token configurableToken = mock(Token.class);
        when(configurableToken.kind()).thenReturn(SyntaxKind.CONFIGURABLE_KEYWORD);
        NodeList<Token> qualifiers = NodeList.from(configurableToken);
        when(variableNode.qualifiers()).thenReturn(qualifiers);

        // Act
        Optional<Artifact> result = transformer.transform(variableNode);

        // Assert
        assertTrue(result.isPresent());
        Artifact artifact = result.get();
        assertEquals(artifact.type(), Artifact.Type.CONFIGURABLE);
    }

    @Test(description = "Test transform connection variable with client")
    public void testTransformConnectionVariable() {
        // Arrange
        ModuleVariableDeclarationNode variableNode = mock(ModuleVariableDeclarationNode.class);
        TypedBindingPatternNode bindingPattern = mock(TypedBindingPatternNode.class);
        when(variableNode.typedBindingPattern()).thenReturn(bindingPattern);
        when(variableNode.qualifiers()).thenReturn(NodeList.from());

        Symbol symbol = mock(VariableSymbol.class);
        TypeReferenceTypeSymbol typeRef = mock(TypeReferenceTypeSymbol.class);
        ClassSymbol classSymbol = mock(ClassSymbol.class);

        when(semanticModel.symbol(variableNode)).thenReturn(Optional.of(symbol));
        when(((VariableSymbol) symbol).typeDescriptor()).thenReturn(typeRef);
        when(typeRef.typeDescriptor()).thenReturn(classSymbol);
        when(classSymbol.qualifiers()).thenReturn(java.util.Set.of(Qualifier.CLIENT));

        // Act
        Optional<Artifact> result = transformer.transform(variableNode);

        // Assert
        assertTrue(result.isPresent());
        Artifact artifact = result.get();
        assertEquals(artifact.type(), Artifact.Type.CONNECTION);
    }

    @Test(description = "Test transform regular variable")
    public void testTransformRegularVariable() {
        // Arrange
        ModuleVariableDeclarationNode variableNode = mock(ModuleVariableDeclarationNode.class);
        TypedBindingPatternNode bindingPattern = mock(TypedBindingPatternNode.class);
        when(variableNode.typedBindingPattern()).thenReturn(bindingPattern);
        when(variableNode.qualifiers()).thenReturn(NodeList.from());
        when(semanticModel.symbol(variableNode)).thenReturn(Optional.empty());

        // Act
        Optional<Artifact> result = transformer.transform(variableNode);

        // Assert
        assertTrue(result.isPresent());
        Artifact artifact = result.get();
        assertEquals(artifact.type(), Artifact.Type.VARIABLE);
    }

    @Test(description = "Test transform type definition")
    public void testTransformTypeDefinition() {
        // Arrange
        TypeDefinitionNode typeNode = mock(TypeDefinitionNode.class);
        IdentifierToken typeName = mock(IdentifierToken.class);
        when(typeName.text()).thenReturn("Person");
        when(typeNode.typeName()).thenReturn(typeName);

        // Act
        Optional<Artifact> result = transformer.transform(typeNode);

        // Assert
        assertTrue(result.isPresent());
        Artifact artifact = result.get();
        assertEquals(artifact.type(), Artifact.Type.TYPE);
        assertEquals(artifact.name(), "Person");
    }

    @Test(description = "Test transform enum declaration")
    public void testTransformEnumDeclaration() {
        // Arrange
        EnumDeclarationNode enumNode = mock(EnumDeclarationNode.class);
        IdentifierToken identifier = mock(IdentifierToken.class);
        when(identifier.text()).thenReturn("Status");
        when(enumNode.identifier()).thenReturn(identifier);

        // Act
        Optional<Artifact> result = transformer.transform(enumNode);

        // Assert
        assertTrue(result.isPresent());
        Artifact artifact = result.get();
        assertEquals(artifact.type(), Artifact.Type.TYPE);
        assertEquals(artifact.name(), "Status");
    }

    @Test(description = "Test transform class definition")
    public void testTransformClassDefinition() {
        // Arrange
        ClassDefinitionNode classNode = mock(ClassDefinitionNode.class);
        IdentifierToken className = mock(IdentifierToken.class);
        when(className.text()).thenReturn("MyClass");
        when(classNode.className()).thenReturn(className);
        when(classNode.members()).thenReturn(NodeList.from());

        // Act
        Optional<Artifact> result = transformer.transform(classNode);

        // Assert
        assertTrue(result.isPresent());
        Artifact artifact = result.get();
        assertEquals(artifact.type(), Artifact.Type.TYPE);
        assertEquals(artifact.name(), "MyClass");
    }

    @Test(description = "Test transform class with method members")
    public void testTransformClassWithMembers() {
        // Arrange
        ClassDefinitionNode classNode = mock(ClassDefinitionNode.class);
        IdentifierToken className = mock(IdentifierToken.class);
        when(className.text()).thenReturn("MyClass");
        when(classNode.className()).thenReturn(className);

        FunctionDefinitionNode methodNode = mock(FunctionDefinitionNode.class);
        IdentifierToken methodName = mock(IdentifierToken.class);
        when(methodName.text()).thenReturn("myMethod");
        when(methodNode.functionName()).thenReturn(methodName);
        when(methodNode.functionBody()).thenReturn(mock(io.ballerina.compiler.syntax.tree.FunctionBodyNode.class));
        when(methodNode.kind()).thenReturn(SyntaxKind.FUNCTION_DEFINITION);
        when(methodNode.qualifierList()).thenReturn(NodeList.from());

        NodeList membersList = NodeList.from(methodNode);
        when(classNode.members()).thenReturn(membersList);

        // Act
        Optional<Artifact> result = transformer.transform(classNode);

        // Assert
        assertTrue(result.isPresent());
        Artifact artifact = result.get();
        assertEquals(artifact.type(), Artifact.Type.TYPE);
        assertEquals(artifact.name(), "MyClass");
        assertFalse(artifact.children().isEmpty());
    }
}