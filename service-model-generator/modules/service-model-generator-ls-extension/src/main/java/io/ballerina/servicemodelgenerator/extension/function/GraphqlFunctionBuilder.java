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
import io.ballerina.servicemodelgenerator.extension.model.Function;
import io.ballerina.servicemodelgenerator.extension.model.GetModelContext;
import io.ballerina.servicemodelgenerator.extension.util.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static io.ballerina.servicemodelgenerator.extension.util.Constants.GRAPHQL;

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
    public String kind() {
        return GRAPHQL;
    }
}
