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
import io.ballerina.flowmodelgenerator.core.model.NodeKind;
import io.ballerina.flowmodelgenerator.core.model.Property;
import io.ballerina.flowmodelgenerator.core.model.SourceBuilder;
import io.ballerina.modelgenerator.commons.FunctionData;
import io.ballerina.modelgenerator.commons.FunctionDataBuilder;
import io.ballerina.modelgenerator.commons.ModuleInfo;
import io.ballerina.modelgenerator.commons.PackageUtil;
import io.ballerina.modelgenerator.commons.ParameterData;
import io.ballerina.projects.Module;
import org.eclipse.lsp4j.TextEdit;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.ballerina.flowmodelgenerator.core.Constants.Workflow.CALL_HUMAN_TASK_METHOD_NAME;
import static io.ballerina.flowmodelgenerator.core.Constants.Workflow.CONTEXT_CLASS_NAME;
import static io.ballerina.flowmodelgenerator.core.Constants.Workflow.HUMAN_TASK_DESCRIPTION;
import static io.ballerina.flowmodelgenerator.core.Constants.Workflow.HUMAN_TASK_LABEL;
import static io.ballerina.flowmodelgenerator.core.Constants.Workflow.WORKFLOW_MODULE;
import static io.ballerina.flowmodelgenerator.core.Constants.Workflow.WORKFLOW_ORG;

/**
 * Represents a workflow human task node. Generates a {@code ctx->awaitHumanTask(...)} call
 * that blocks until a human completes the task or the optional timeout elapses.
 *
 * <p>Generated source example:
 * <pre>{@code
 * ApprovalDecision result = check ctx->awaitHumanTask("approveExpense", ["FINANCE_APPROVER"],
 *         payload = {"amount": 1200},
 *         title = "Approve order",
 *         timeout = {hours: 24});
 * }</pre>
 *
 * @since 1.9.0
 */
public class HumanTaskBuilder extends CallBuilder {

    public static final String LABEL = HUMAN_TASK_LABEL;
    public static final String DESCRIPTION = HUMAN_TASK_DESCRIPTION;
    public static final String DEFAULT_RETURN_TYPE = "anydata";

    /**
     * Property key for the inferred result type parameter ({@code typedesc<anydata> T = <>}).
     * Matches the convention used by the builtin Call REST activity so the form renders a rich,
     * record-field-aware type selector that drives the LHS databinding type in {@link #toSource}.
     */
    public static final String INFERRED_TYPE_KEY = "T";

    @Override
    protected NodeKind getFunctionNodeKind() {
        return NodeKind.HUMAN_TASK;
    }

    @Override
    protected FunctionData.Kind getFunctionResultKind() {
        return FunctionData.Kind.REMOTE;
    }

    @Override
    public void setConcreteConstData() {
        metadata().label(LABEL).description(DESCRIPTION);
        codedata()
                .node(NodeKind.HUMAN_TASK)
                .org(WORKFLOW_ORG)
                .module(WORKFLOW_MODULE)
                .object(CONTEXT_CLASS_NAME)
                .symbol(CALL_HUMAN_TASK_METHOD_NAME);
    }

    @Override
    public void setConcreteTemplateData(TemplateContext context) {
        metadata().label(LABEL).description(DESCRIPTION);
        codedata()
                .node(NodeKind.HUMAN_TASK)
                .org(WORKFLOW_ORG)
                .module(WORKFLOW_MODULE)
                .object(CONTEXT_CLASS_NAME)
                .symbol(CALL_HUMAN_TASK_METHOD_NAME);

        boolean fallbackTemplate = false;
        try {
            ModuleInfo workflowModuleInfo = new ModuleInfo(WORKFLOW_ORG, WORKFLOW_MODULE, WORKFLOW_MODULE, null);
            FunctionData functionData = new FunctionDataBuilder()
                    .name(CALL_HUMAN_TASK_METHOD_NAME)
                    .moduleInfo(workflowModuleInfo)
                    .parentSymbolType(CONTEXT_CLASS_NAME)
                    .functionResultKind(FunctionData.Kind.REMOTE)
                    .project(PackageUtil.loadProject(context.workspaceManager(), context.filePath()))
                    .userModuleInfo(moduleInfo)
                    .workspaceManager(context.workspaceManager())
                    .filePath(context.filePath())
                    .build();

            if (functionData == null || functionData.parameters() == null || functionData.parameters().isEmpty()) {
                fallbackTemplate = true;
            } else {
                Module module = context.workspaceManager().module(context.filePath()).orElse(null);
                // Build each parameter (taskName, userRoles, payload, title, description, timeout) with its
                // real compiler-derived type metadata, and let the inferred {@code typedesc<anydata> T}
                // parameter become a rich result-type selector (record-field-selector for record types) —
                // the same mechanism the builtin Call REST activity uses for its databinding type.
                setParameterProperties(functionData, module);

                // The inferred result type defaults to anydata until the user picks a concrete type.
                Property resultType = properties().build().get(INFERRED_TYPE_KEY);
                if (resultType != null && (resultType.value() == null || resultType.value().toString().isEmpty())) {
                    properties().build().put(INFERRED_TYPE_KEY,
                            Property.Builder.copyFrom(resultType).value(DEFAULT_RETURN_TYPE).build());
                }
            }
        } catch (IllegalStateException e) {
            // awaitHumanTask may not be resolvable from project dependencies yet.
            // Use a stable, static form so opening/editing HUMAN_TASK nodes still works.
            fallbackTemplate = true;
        }

        if (fallbackTemplate) {
            setFallbackHumanTaskProperties();
        }

        // Apply friendly form labels/descriptions without discarding the rich type metadata above.
        relabelHumanTaskFormProperties(properties().build());

        properties().data(Property.RESULT_NAME, context.getAllVisibleSymbolNames(),
                Property.RESULT_NAME, Property.RESULT_DOC, false);

        properties().checkError(true);
    }

    private void setFallbackHumanTaskProperties() {
        properties().custom()
                .metadata()
                    .label("Task Name")
                    .description("Identifies the task type")
                    .stepOut()
                .type().fieldType(Property.ValueType.TEXT).ballerinaType("string").selected(true).stepOut()
                .type().fieldType(Property.ValueType.EXPRESSION).ballerinaType("string").selected(false).stepOut()
                .codedata().kind(ParameterData.Kind.REQUIRED.name()).originalName("taskName").stepOut()
                .editable(true)
                .stepOut()
                .addProperty("taskName");

        properties().custom()
                .metadata()
                    .label("User Roles")
                    .description("One or more roles permitted to complete this task")
                    .stepOut()
                .type().fieldType(Property.ValueType.EXPRESSION).ballerinaType("string|string[]").selected(true)
                    .stepOut()
                .codedata().kind(ParameterData.Kind.REQUIRED.name()).originalName("userRoles").stepOut()
                .editable(true)
                .stepOut()
                .addProperty("userRoles");

        properties().custom()
                .metadata()
                    .label("Payload")
                    .description("Read-only JSON object shown alongside the form")
                    .stepOut()
                .type().fieldType(Property.ValueType.EXPRESSION).ballerinaType("map<json>").selected(true).stepOut()
                .codedata().kind(ParameterData.Kind.DEFAULTABLE.name()).originalName("payload").stepOut()
                .editable(true)
                .optional(true)
                .stepOut()
                .addProperty("payload");

        properties().custom()
                .metadata()
                    .label("Title")
                    .description("Short summary shown in the inbox")
                    .stepOut()
                .type().fieldType(Property.ValueType.TEXT).ballerinaType("string?").selected(true).stepOut()
                .type().fieldType(Property.ValueType.EXPRESSION).ballerinaType("string?").selected(false).stepOut()
                .codedata().kind(ParameterData.Kind.DEFAULTABLE.name()).originalName("title").stepOut()
                .editable(true)
                .optional(true)
                .stepOut()
                .addProperty("title");

        properties().custom()
                .metadata()
                    .label("Description")
                    .description("Additional context shown alongside the form")
                    .stepOut()
                .type().fieldType(Property.ValueType.TEXT).ballerinaType("string?").selected(true).stepOut()
                .type().fieldType(Property.ValueType.EXPRESSION).ballerinaType("string?").selected(false).stepOut()
                .codedata().kind(ParameterData.Kind.DEFAULTABLE.name()).originalName("description").stepOut()
                .editable(true)
                .optional(true)
                .stepOut()
                .addProperty("description");

        properties().custom()
                .metadata()
                    .label("Timeout")
                    .description("Maximum time to wait; omit to wait indefinitely")
                    .stepOut()
                .type().fieldType(Property.ValueType.EXPRESSION).ballerinaType("time:Duration?").selected(true)
                    .stepOut()
                .codedata().kind(ParameterData.Kind.DEFAULTABLE.name()).originalName("timeout").stepOut()
                .editable(true)
                .optional(true)
                .stepOut()
                .addProperty("timeout");

        // Inferred result type ("T") — bare type selector when the module signature is unavailable.
        properties().custom()
                .metadata()
                    .label(Property.RESULT_TYPE_LABEL)
                    .description("The expected return type of the human task result")
                    .stepOut()
                .type().fieldType(Property.ValueType.TYPE).ballerinaType(DEFAULT_RETURN_TYPE).selected(true).stepOut()
                .codedata().kind(ParameterData.Kind.PARAM_FOR_TYPE_INFER.name()).originalName(INFERRED_TYPE_KEY)
                    .stepOut()
                .value(DEFAULT_RETURN_TYPE)
                .editable(true)
                .stepOut()
                .addProperty(INFERRED_TYPE_KEY);
    }

    /**
     * Applies friendly form labels and descriptions to the {@code awaitHumanTask} properties without
     * discarding their compiler-derived type metadata (type symbols, imports, record-field selectors).
     * Shared by the template ({@link #setConcreteTemplateData}) and the source re-read path in
     * {@code CodeAnalyzer}, so both render an identical, type-aware form.
     *
     * @param properties the live property map to relabel in place
     */
    public static void relabelHumanTaskFormProperties(Map<String, Property> properties) {
        relabel(properties, "taskName", "Task Name", "Identifies the task type");
        relabel(properties, "userRoles", "User Roles", "One or more roles permitted to complete this task");
        relabel(properties, "payload", "Payload", "Read-only JSON object shown alongside the form");
        relabel(properties, "title", "Title", "Short summary shown in the inbox");
        relabel(properties, "description", "Description", "Additional context shown alongside the form");
        relabel(properties, "timeout", "Timeout", "Maximum time to wait; omit to wait indefinitely");
        relabel(properties, INFERRED_TYPE_KEY, Property.RESULT_TYPE_LABEL,
                "The expected return type of the human task result");
    }

    private static void relabel(Map<String, Property> properties, String key, String label, String description) {
        Property existing = properties.get(key);
        if (existing == null) {
            return;
        }
        properties.put(key, Property.Builder.copyFrom(existing)
                .metadata().label(label).description(description).stepOut()
                .build());
    }

    @Override
    public Map<Path, List<TextEdit>> toSource(SourceBuilder sourceBuilder) {
        Optional<Property> typeProp = sourceBuilder.getProperty(INFERRED_TYPE_KEY);
        Optional<Property> variableProp = sourceBuilder.getProperty(Property.VARIABLE_KEY);
        Optional<Property> checkErrorProp = sourceBuilder.getProperty(Property.CHECK_ERROR_KEY);

        String resultType = normalizeResultType(typeProp
                .map(p -> p.value() != null ? p.value().toString() : DEFAULT_RETURN_TYPE)
                .orElse(DEFAULT_RETURN_TYPE));
        String variableName = variableProp
                .map(p -> p.value() != null ? p.value().toString() : "result")
                .orElse("result");
        boolean useCheck = checkErrorProp
                .map(p -> p.value() == null || !"false".equals(p.value().toString()))
                .orElse(true);

        // Required positional args
        String taskName = sourceBuilder.getProperty("taskName")
                .map(p -> p.value() != null ? p.value().toString() : "\"\"")
                .orElse("\"\"");
        String userRoles = sourceBuilder.getProperty("userRoles")
                .map(p -> p.value() != null ? p.value().toString() : "\"admin\"")
                .orElse("\"admin\"");

        // Optional named args (only when the user provided a value)
        List<String> callArgs = new ArrayList<>();
        callArgs.add(taskName);
        callArgs.add(userRoles);
        addNamedArg(sourceBuilder, callArgs, "payload");
        addNamedArg(sourceBuilder, callArgs, "title");
        addNamedArg(sourceBuilder, callArgs, "description");
        addNamedArg(sourceBuilder, callArgs, "timeout");

        String ctxParamName = ActivityCallBuilder.resolveContextParamName(sourceBuilder);

        sourceBuilder.token()
                .name(useCheck ? resultType : resultType + "|error")
                .whiteSpace()
                .name(variableName)
                .whiteSpace()
                .keyword(SyntaxKind.EQUAL_TOKEN);

        if (useCheck) {
            sourceBuilder.token().keyword(SyntaxKind.CHECK_KEYWORD);
        }

        sourceBuilder.token()
                .name(ctxParamName)
                .keyword(SyntaxKind.RIGHT_ARROW_TOKEN)
                .name(CALL_HUMAN_TASK_METHOD_NAME)
                .keyword(SyntaxKind.OPEN_PAREN_TOKEN)
                .name(String.join(", ", callArgs))
                .keyword(SyntaxKind.CLOSE_PAREN_TOKEN)
                .endOfStatement();

        return sourceBuilder
                .textEdit()
                .acceptImport(WORKFLOW_ORG, WORKFLOW_MODULE)
                .build();
    }

    private static void addNamedArg(SourceBuilder sourceBuilder, List<String> args, String key) {
        sourceBuilder.getProperty(key).ifPresent(p -> {
            if (p.value() != null && !p.value().toString().isEmpty()) {
                args.add(key + " = " + p.value().toString());
            }
        });
    }

    /**
     * Strips any trailing {@code |error} from the stored result type so that the unchecked branch of
     * {@link #toSource} (which appends {@code |error}) does not produce a duplicated — and on each
     * round-trip, accumulating — {@code ...|error|error} union. When a human task binds to a generic
     * {@code T|error} variable, the inferred-type derivation keeps the {@code |error}; the result type
     * field is meant to hold only the success type {@code T}.
     */
    private static String normalizeResultType(String resultType) {
        String normalized = resultType.strip();
        while (normalized.endsWith("|error")) {
            normalized = normalized.substring(0, normalized.length() - "|error".length()).strip();
        }
        return normalized.isEmpty() ? DEFAULT_RETURN_TYPE : normalized;
    }
}
