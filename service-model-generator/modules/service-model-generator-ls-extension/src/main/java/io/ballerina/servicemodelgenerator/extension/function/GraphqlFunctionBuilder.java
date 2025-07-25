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

package io.ballerina.servicemodelgenerator.extension.function;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.modelgenerator.commons.Annotation;
import io.ballerina.modelgenerator.commons.ServiceDatabaseManager;
import io.ballerina.servicemodelgenerator.extension.model.Function;
import io.ballerina.servicemodelgenerator.extension.model.GetModelContext;
import io.ballerina.servicemodelgenerator.extension.model.ModelFromSourceContext;
import io.ballerina.servicemodelgenerator.extension.model.Value;
import io.ballerina.servicemodelgenerator.extension.util.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static io.ballerina.servicemodelgenerator.extension.ServiceModelGeneratorConstants.KIND_RESOURCE;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.BALLERINA;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.GRAPHQL;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.OBJECT_METHOD;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.RESOURCE;
import static io.ballerina.servicemodelgenerator.extension.util.Utils.getFunctionModel;
import static io.ballerina.servicemodelgenerator.extension.util.Utils.updateValue;

public class GraphqlFunctionBuilder extends AbstractFunctionBuilder {
    private static final String GRAPHQL_FUNCTION_MODEL_LOCATION = "functions/graphql_%s.json";

    @Override
    public Optional<Function> getModelTemplate(GetModelContext context) {
        String resourcePath =  String.format(GRAPHQL_FUNCTION_MODEL_LOCATION, context.functionType());
        InputStream resourceStream = Utils.class.getClassLoader().getResourceAsStream(resourcePath);
        if (resourceStream == null) {
            return Optional.empty();
        }

        try (JsonReader reader = new JsonReader(new InputStreamReader(resourceStream, StandardCharsets.UTF_8))) {
            return Optional.of(new Gson().fromJson(reader, Function.class));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    @Override
    public Function getModelFromSource(ModelFromSourceContext context) {
        ServiceDatabaseManager databaseManager = ServiceDatabaseManager.getInstance();
        List<Annotation> annotationAttachments = databaseManager.
                getAnnotationAttachments(BALLERINA, GRAPHQL, OBJECT_METHOD);
        Map<String, Value> annotations = Function.createAnnotationsMap(annotationAttachments);
        Function functionModel = getFunctionModel((FunctionDefinitionNode) context.node(), context.semanticModel(),
                true, annotations);
        functionModel.setEditable(true);

        if (functionModel.getKind().equals(KIND_RESOURCE)) {
            Optional<Function> resourceFunctionOp = getGraphqlFunctionModel(RESOURCE);
            if (resourceFunctionOp.isPresent()) {
                Function resourceFunction = resourceFunctionOp.get();
                if (resourceFunction.getReturnType().getResponses().size() > 1) {
                    resourceFunction.getReturnType().getResponses().remove(1);
                }
                updateFunctionInfo(resourceFunction, functionModel);
                return resourceFunction;
            }
        } else {
            functionModel.setAnnotations(null);
            functionModel.getAccessor().setEnabled(false);
        }
        return functionModel;
    }

    private static Optional<Function> getGraphqlFunctionModel(String functionType) {
        InputStream resourceStream = Utils.class.getClassLoader()
                .getResourceAsStream(String.format(GRAPHQL_FUNCTION_MODEL_LOCATION, functionType));
        if (resourceStream == null) {
            return Optional.empty();
        }

        try (JsonReader reader = new JsonReader(new InputStreamReader(resourceStream, StandardCharsets.UTF_8))) {
            return Optional.of(new Gson().fromJson(reader, Function.class));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private static void updateFunctionInfo(Function functionModel, Function commonFunction) {
        functionModel.setEditable(commonFunction.isEditable());
        functionModel.setEnabled(true);
        functionModel.setKind(commonFunction.getKind());
        functionModel.setCodedata(commonFunction.getCodedata());
        updateValue(functionModel.getAccessor(), commonFunction.getAccessor());
        updateValue(functionModel.getName(), commonFunction.getName());
        updateValue(functionModel.getReturnType(), commonFunction.getReturnType());
        Set<String> existingTypes = functionModel.getParameters().stream()
                .map(parameter -> parameter.getType().getValue())
                .collect(Collectors.toSet());
        commonFunction.getParameters().stream()
                .filter(commonParam -> !existingTypes.contains(commonParam.getType().getValue()))
                .forEach(functionModel::addParameter);
    }

    @Override
    public String kind() {
        return GRAPHQL;
    }
}
