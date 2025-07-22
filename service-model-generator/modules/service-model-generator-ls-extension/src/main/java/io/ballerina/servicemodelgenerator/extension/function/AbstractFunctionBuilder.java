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
import io.ballerina.servicemodelgenerator.extension.model.AddModelContext;
import io.ballerina.servicemodelgenerator.extension.model.Function;
import io.ballerina.servicemodelgenerator.extension.model.GetModelContext;
import io.ballerina.servicemodelgenerator.extension.model.ModelFromSourceContext;
import io.ballerina.servicemodelgenerator.extension.model.NodeBuilder;
import io.ballerina.servicemodelgenerator.extension.model.UpdateModelContext;
import io.ballerina.servicemodelgenerator.extension.util.Utils;
import org.eclipse.lsp4j.TextEdit;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Represents the abstract function builder of the service model generator.
 *
 * @since 1.2.0
 */
public abstract class AbstractFunctionBuilder implements NodeBuilder<Function> {

    private static final String DEFAULT_FUNCTION_MODEL_LOCATION = "functions/%s_%s.json";

    /**
     * Get the model template for a given function.
     *
     * @param context the context information for retrieving the functional model template
     * @return the model template
     */
    @Override
    public Optional<Function> getModelTemplate(GetModelContext context) {
        String resourcePath =  String.format(DEFAULT_FUNCTION_MODEL_LOCATION, context.serviceType(),
                context.functionType());
        InputStream resourceStream = Utils.class.getClassLoader()
                .getResourceAsStream(resourcePath);
        if (resourceStream == null) {
            return Optional.empty();
        }

        try (JsonReader reader = new JsonReader(new InputStreamReader(resourceStream, StandardCharsets.UTF_8))) {
            return Optional.of(new Gson().fromJson(reader, Function.class));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    /**
     * Get the list of text edits for the given function model for addition.
     *
     * @param context the context information for adding the service
     * @return a map of file paths to lists of text edits
     */
    @Override
    public Map<String, List<TextEdit>> addModel(AddModelContext context) throws Exception {
        return Map.of();
    }

    /**
     * Get the list of text edits for the given function for updating.
     *
     * @param context the context information for updating the service
     * @return a map of file paths to lists of text edits
     */
    @Override
    public Map<String, List<TextEdit>> updateModel(UpdateModelContext context) {
        return Map.of();
    }

    /**
     * Get the function model from the source code.
     *
     * @param context the context information for extracting the function model
     * @return the ser extracted from the source code
     */
    @Override
    public Function getModelFromSource(ModelFromSourceContext context) {
        return null;
    }

    /**
     * @return kind of the function model
     */
    @Override
    public String kind() {
        return "";
    }
}
