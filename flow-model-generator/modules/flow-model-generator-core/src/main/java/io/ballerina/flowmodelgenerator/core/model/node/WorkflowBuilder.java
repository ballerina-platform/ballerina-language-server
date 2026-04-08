/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com)
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

package io.ballerina.flowmodelgenerator.core.model.node;

import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.flowmodelgenerator.core.model.NodeBuilder;
import io.ballerina.flowmodelgenerator.core.model.NodeKind;
import io.ballerina.flowmodelgenerator.core.model.Property;
import io.ballerina.flowmodelgenerator.core.model.SourceBuilder;
import io.ballerina.modelgenerator.commons.ModuleInfo;
import io.ballerina.modelgenerator.commons.PackageUtil;
import io.ballerina.tools.text.LineRange;
import org.eclipse.lsp4j.TextEdit;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.ballerina.flowmodelgenerator.core.Constants.Workflow.DEFAULT_INPUT_PARAM_NAME;
import static io.ballerina.flowmodelgenerator.core.Constants.Workflow.WORKFLOW_MODULE;
import static io.ballerina.flowmodelgenerator.core.Constants.Workflow.WORKFLOW_ORG;

/**
 * Represents the properties of a workflow process function definition node.
 *
 * @since 1.8.0
 */
public class WorkflowBuilder extends FunctionDefinitionBuilder {

    public static final String LABEL = "Workflow Process Function";
    public static final String DESCRIPTION = "Define a workflow process function";

    public static final String INPUT_KEY = "inputType";
    public static final String INPUT_LABEL = "Input Type";
    public static final String INPUT_DOC = "Type of the input data to the workflow";
    public static final String ANYDATA_TYPE = "anydata";

    // Hidden key that preserves the workflow:Context parameter text from the original source.
    // Not shown in the edit form; re-emitted verbatim by toSource() so it is never dropped on save.
    public static final String CONTEXT_PARAM_KEY = "workflowContextParam";

    @Override
    public void setConcreteConstData() {
        metadata().label(LABEL).description(DESCRIPTION);
        codedata()
                .node(NodeKind.WORKFLOW)
                .org(WORKFLOW_ORG)
                .module(WORKFLOW_MODULE);
    }

    @Override
    public void setConcreteTemplateData(TemplateContext context) {
        ModuleInfo workflowModuleInfo = new ModuleInfo(WORKFLOW_ORG, WORKFLOW_MODULE, WORKFLOW_MODULE, null);
        PackageUtil.pullModuleAndNotify(context.lsClientLogger(), workflowModuleInfo);

        // Add function name
        properties().functionNameTemplate("workflow", context.getAllVisibleSymbolNames());

        // Add function description
        properties().functionDescription("");

        // Add input property with WORKFLOW_INPUT_TYPE
        properties().custom()
                .metadata()
                    .label(INPUT_LABEL)
                    .description(INPUT_DOC)
                    .stepOut()
                .type()
                    .fieldType(Property.ValueType.TYPE)
                    .ballerinaType(ANYDATA_TYPE)
                .stepOut()
                .value("")
                .editable(true)
                .optional(true)
                .stepOut()
                .addProperty(INPUT_KEY);

        // Return type
        properties().returnType("error?", null, true);
        properties().returnDescription("");
    }

    /**
     * Sets the mandatory properties for an existing workflow function node being loaded for editing.
     * Mirrors the property shape of {@link #setConcreteTemplateData} so the edit form is consistent.
     *
     * @param nodeBuilder       the node builder to populate
     * @param returnType        the function's return type text
     * @param description       the function-level doc comment
     * @param returnDescription the return-value doc comment
     * @param inputType         the workflow input parameter type (empty string if none)
     * @param contextParam      the full workflow:Context parameter text (e.g. "workflow:Context ctx"),
     *                          or empty string if the original function had no Context parameter
     */
    public static void setMandatoryProperties(NodeBuilder nodeBuilder, String returnType, String description,
                                              String returnDescription, String inputType, String contextParam) {
        nodeBuilder.properties().functionDescription(description);
        nodeBuilder.properties().custom()
                .metadata()
                    .label(INPUT_LABEL)
                    .description(INPUT_DOC)
                    .stepOut()
                .type()
                    .fieldType(Property.ValueType.TYPE)
                    .ballerinaType(ANYDATA_TYPE)
                .stepOut()
                .value(inputType)
                .editable(true)
                .optional(true)
                .stepOut()
                .addProperty(INPUT_KEY);
        // Store the original context parameter text as a hidden property so toSource() can re-emit it
        // unchanged, preserving the workflow:Context parameter without exposing it to the edit form.
        if (contextParam != null && !contextParam.isEmpty()) {
            nodeBuilder.properties().custom()
                    .metadata().label("").description("").stepOut()
                    .type().fieldType(Property.ValueType.TEXT).stepOut()
                    .value(contextParam)
                    .hidden()
                    .optional(true)
                    .editable(false)
                    .stepOut()
                    .addProperty(CONTEXT_PARAM_KEY);
        }
        nodeBuilder.properties()
                .returnType(returnType, null, true)
                .returnDescription(returnDescription);
    }

    @Override
    public Map<Path, List<TextEdit>> toSource(SourceBuilder sourceBuilder) {
        Optional<Property> optDescription = sourceBuilder.getProperty(Property.FUNCTION_NAME_DESCRIPTION_KEY);
        String description = optDescription.map(property -> property.value().toString()).orElse("");

        Optional<Property> funcNameProperty = sourceBuilder.getProperty(Property.FUNCTION_NAME_KEY);
        if (funcNameProperty.isEmpty()) {
            throw new IllegalStateException("Function name is not present");
        }
        String funcName = funcNameProperty.get().value().toString();

        if (!description.isEmpty()) {
            sourceBuilder.token().descriptionDoc(description);
        }

        sourceBuilder.token()
                .name("@workflow:Workflow")
                .newLine()
                .keyword(SyntaxKind.FUNCTION_KEYWORD)
                .name(funcName)
                .keyword(SyntaxKind.OPEN_PAREN_TOKEN);

        Optional<Property> contextProp = sourceBuilder.getProperty(CONTEXT_PARAM_KEY);
        String contextParamText = contextProp.map(p -> p.value().toString()).orElse("");

        Optional<Property> inputProperty = sourceBuilder.getProperty(INPUT_KEY);
        String inputTypeName = inputProperty.map(p -> p.value().toString()).orElse("");

        if (!contextParamText.isEmpty() && !inputTypeName.isEmpty()) {
            sourceBuilder.token()
                    .name(contextParamText)
                    .keyword(SyntaxKind.COMMA_TOKEN)
                    .whiteSpace()
                    .name(inputTypeName)
                    .whiteSpace()
                    .name(DEFAULT_INPUT_PARAM_NAME);
        } else if (!contextParamText.isEmpty()) {
            sourceBuilder.token().name(contextParamText);
        } else if (!inputTypeName.isEmpty()) {
            sourceBuilder.token()
                    .name(inputTypeName)
                    .whiteSpace()
                    .name(DEFAULT_INPUT_PARAM_NAME);
        }

        sourceBuilder.token().keyword(SyntaxKind.CLOSE_PAREN_TOKEN);

        // Return type
        Optional<Property> returnType = sourceBuilder.getProperty(Property.TYPE_KEY);
        if (returnType.isPresent()) {
            String typeName = returnType.get().value().toString();
            if (!typeName.isEmpty()) {
                sourceBuilder.token()
                        .keyword(SyntaxKind.RETURNS_KEYWORD)
                        .name(typeName);
            }
        }

        // New workflow nodes may still carry a line range from the insertion context (for example, diagram creation
        // from main.bal). Treat `isNew` as the source of truth so new workflow declarations are generated fully in
        // functions.bal with imports and an empty body.
        LineRange lineRange = sourceBuilder.flowNode.codedata().lineRange();
        if (Boolean.TRUE.equals(sourceBuilder.flowNode.codedata().isNew()) || lineRange == null) {
            sourceBuilder
                    .token()
                        .openBrace()
                        .closeBrace()
                        .stepOut()
                    .textEdit(SourceBuilder.SourceKind.DECLARATION)
                    .acceptImport();
        } else {
            sourceBuilder
                    .token().skipFormatting().stepOut()
                    .textEdit();
        }

        return sourceBuilder.build();
    }
}
