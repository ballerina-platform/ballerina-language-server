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

package io.ballerina.servicemodelgenerator.extension.builder.function;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.MappingConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.MappingFieldNode;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.SpecificFieldNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.servicemodelgenerator.extension.model.Codedata;
import io.ballerina.servicemodelgenerator.extension.model.Function;
import io.ballerina.servicemodelgenerator.extension.model.Parameter;
import io.ballerina.servicemodelgenerator.extension.model.Service;
import io.ballerina.servicemodelgenerator.extension.model.Value;
import io.ballerina.servicemodelgenerator.extension.model.context.AddModelContext;
import io.ballerina.servicemodelgenerator.extension.model.context.ModelFromSourceContext;
import io.ballerina.servicemodelgenerator.extension.model.context.UpdateModelContext;
import org.eclipse.lsp4j.TextEdit;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.ballerina.servicemodelgenerator.extension.util.Constants.SMB;

/**
 * Represents the SMB function builder of the service model generator.
 * Handles fileNamePattern annotations for SMB functions.
 *
 * @since 1.5.0
 */
public final class SMBFunctionBuilder extends AbstractFunctionBuilder {

    private static final String SMB_SERVICE_JSON = "services/smb_service.json";
    private static final String FUNCTION_CONFIG = "FunctionConfig";
    private static final String FILE_NAME_PATTERN = "fileNamePattern";
    private static final String ANNOTATION_ATTACHMENT = "ANNOTATION_ATTACHMENT";
    private static final String DATA_BINDING = "DATA_BINDING";
    private static final String STREAM = "stream";
    private static final String CONTENT = "content";

    @Override
    public Map<String, List<TextEdit>> updateModel(UpdateModelContext context) {
        processFileNamePattern(context.function());
        return super.updateModel(context);
    }

    @Override
    public Map<String, List<TextEdit>> addModel(AddModelContext context) throws Exception {
        processFileNamePattern(context.function());
        return super.addModel(context);
    }

    @Override
    public Function getModelFromSource(ModelFromSourceContext context) {
        Function functionModel = super.getModelFromSource(context);
        if (context.node() instanceof FunctionDefinitionNode functionNode) {
            functionModel = mergeWithTemplate(functionNode, functionModel);
            updateFileNamePatternFromAnnotation(functionNode, functionModel);
        }
        return functionModel;
    }

    /**
     * Processes the fileNamePattern property and converts it to an annotation property.
     * If fileNamePattern is set and non-empty, it is converted to a @smb:FunctionConfig annotation.
     *
     * @param function the function model to process
     */
    private void processFileNamePattern(Function function) {
        Map<String, Value> properties = function.getProperties();

        Value fileNamePatternProp = properties.get(FILE_NAME_PATTERN);
        if (fileNamePatternProp == null || !fileNamePatternProp.isEnabled()) {
            properties.remove(FILE_NAME_PATTERN);
            return;
        }

        String pattern = fileNamePatternProp.getValue();
        if (pattern == null || pattern.isEmpty() || "\"\"".equals(pattern.trim())) {
            properties.remove(FILE_NAME_PATTERN);
            return;
        }

        properties.remove(FILE_NAME_PATTERN);

        Codedata codedata = new Codedata.Builder()
                .setType(ANNOTATION_ATTACHMENT)
                .setOriginalName(FUNCTION_CONFIG)
                .setModuleName(SMB)
                .build();

        String annotationValue = " {\n    fileNamePattern: " + pattern + "\n}";

        Value annotationProperty = new Value.ValueBuilder()
                .setCodedata(codedata)
                .value(annotationValue)
                .enabled(true)
                .editable(false)
                .optional(false)
                .setAdvanced(false)
                .build();

        properties.put(FUNCTION_CONFIG, annotationProperty);
    }

    /**
     * Merges source function details with the SMB template function model.
     * This ensures SMB-specific properties like fileNamePattern are available in edit mode.
     */
    private Function mergeWithTemplate(FunctionDefinitionNode functionNode, Function sourceFunction) {
        Optional<Function> templateFunction = getTemplateFunction(functionNode.functionName().text().trim());
        if (templateFunction.isEmpty()) {
            return sourceFunction;
        }

        Function mergedFunction = templateFunction.get();
        mergedFunction.setEnabled(true);
        mergedFunction.setEditable(true);
        mergedFunction.setCodedata(sourceFunction.getCodedata());
        mergedFunction.setDocumentation(sourceFunction.getDocumentation());

        if (sourceFunction.getReturnType() != null && mergedFunction.getReturnType() != null) {
            mergedFunction.getReturnType().setValue(sourceFunction.getReturnType().getValue());
        }

        enableParameters(sourceFunction, mergedFunction);
        updateDatabindingParameter(sourceFunction, mergedFunction);

        if (mergedFunction.getProperties().containsKey(STREAM)) {
            setStreamProperty(mergedFunction, sourceFunction);
        }
        return mergedFunction;
    }

    private Optional<Function> getTemplateFunction(String functionName) {
        InputStream resourceStream = SMBFunctionBuilder.class.getClassLoader().getResourceAsStream(SMB_SERVICE_JSON);
        if (resourceStream == null) {
            return Optional.empty();
        }

        try (JsonReader reader = new JsonReader(new InputStreamReader(resourceStream, StandardCharsets.UTF_8))) {
            Service service = new Gson().fromJson(reader, Service.class);
            if (service == null || service.getFunctions() == null) {
                return Optional.empty();
            }
            return service.getFunctions().stream()
                    .filter(function ->
                        function.getName() != null && functionName.equals(function.getName().getValue()))
                    .findFirst();
        } catch (IOException ignored) {
            return Optional.empty();
        }
    }

    private static void enableParameters(Function sourceFunc, Function modelFunc) {
        if (modelFunc.getParameters() == null || sourceFunc.getParameters() == null) {
            return;
        }
        modelFunc.getParameters().forEach(parameter -> parameter.setEnabled(false));
        for (Parameter sourceParam : sourceFunc.getParameters()) {
            modelFunc.getParameters().stream()
                    .filter(modelParam -> modelParam.getType().getValue().equals(sourceParam.getType().getValue())
                            || DATA_BINDING.equals(modelParam.getKind())
                            || CONTENT.equals(modelParam.getName().getValue()))
                    .forEach(modelParam -> modelParam.setEnabled(true));
        }
    }

    private static void updateDatabindingParameter(Function sourceFunc, Function modelFunc) {
        if (sourceFunc.getParameters() == null || sourceFunc.getParameters().isEmpty()
                || modelFunc.getParameters() == null || modelFunc.getParameters().isEmpty()) {
            return;
        }

        Parameter sourceParam = sourceFunc.getParameters().getFirst();
        Parameter modelParam = modelFunc.getParameters().getFirst();
        if (modelParam.getType() != null && DATA_BINDING.equals(modelParam.getKind())) {
            if (sourceParam.getName() != null && modelParam.getName() != null) {
                modelParam.getName().setValue(sourceParam.getName().getValue());
            }
            if (sourceParam.getType() != null && modelParam.getType() != null) {
                modelParam.getType().setValue(sourceParam.getType().getValue());
            }
        }
    }

    private static void setStreamProperty(Function modelFunc, Function sourceFunc) {
        boolean isStream = false;
        if (sourceFunc.getParameters() != null && !sourceFunc.getParameters().isEmpty()) {
            Parameter firstParam = sourceFunc.getParameters().getFirst();
            if (firstParam.getType() != null) {
                String paramType = firstParam.getType().getValue();
                if (paramType != null && paramType.startsWith("stream<")) {
                    isStream = true;
                }
            }
        }

        Value streamProperty = modelFunc.getProperties().get(STREAM);
        if (streamProperty == null) {
            return;
        }
        streamProperty.setValue(String.valueOf(isStream));
        streamProperty.setEnabled(isStream);
        streamProperty.setEditable(false);
    }

    /**
     * Reads the @smb:FunctionConfig annotation from a function node and populates
     * the fileNamePattern property in the function model.
     *
     * @param functionNode the function definition node from the source
     * @param modelFunc    the function model to update
     */
    private void updateFileNamePatternFromAnnotation(FunctionDefinitionNode functionNode, Function modelFunc) {
        Value fileNamePatternProp = modelFunc.getProperties().get(FILE_NAME_PATTERN);
        if (fileNamePatternProp == null) {
            return;
        }

        if (functionNode.metadata().isEmpty()) {
            fileNamePatternProp.setEnabled(false);
            return;
        }

        Optional<AnnotationNode> functionConfig = findAnnotationBySuffix(
                functionNode.metadata().get().annotations(), FUNCTION_CONFIG);
        if (functionConfig.isEmpty()) {
            fileNamePatternProp.setEnabled(false);
            return;
        }

        Optional<MappingConstructorExpressionNode> annotValue = functionConfig.get().annotValue();
        if (annotValue.isEmpty()) {
            fileNamePatternProp.setEnabled(false);
            return;
        }

        boolean found = false;
        for (MappingFieldNode field : annotValue.get().fields()) {
            if (field.kind() != SyntaxKind.SPECIFIC_FIELD) {
                continue;
            }
            SpecificFieldNode specificField = (SpecificFieldNode) field;
            String fieldName = specificField.fieldName().toString().trim();
            if (FILE_NAME_PATTERN.equals(fieldName)) {
                specificField.valueExpr().ifPresent(expr -> {
                    fileNamePatternProp.setValue(expr.toSourceCode().trim());
                    fileNamePatternProp.setEnabled(true);
                });
                found = true;
            }
        }

        if (!found) {
            fileNamePatternProp.setEnabled(false);
        }
    }

    private Optional<AnnotationNode> findAnnotationBySuffix(NodeList<AnnotationNode> annotations, String suffix) {
        for (AnnotationNode annotation : annotations) {
            String annotationText = annotation.annotReference().toString().trim();
            if (annotationText.endsWith(suffix)) {
                return Optional.of(annotation);
            }
        }
        return Optional.empty();
    }

    @Override
    public String kind() {
        return SMB;
    }
}
