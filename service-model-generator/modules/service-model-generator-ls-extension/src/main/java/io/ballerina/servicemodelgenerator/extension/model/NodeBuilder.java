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

package io.ballerina.servicemodelgenerator.extension.model;

import org.eclipse.lsp4j.TextEdit;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface NodeBuilder<T> {

    /**
     * Get the model template for the given service type or function type.
     *
     * @return the model template
     */
    Optional<T> getModelTemplate(String moduleName);

    /**
     * Get the list of text edits for the given model for addition.
     *
     * @param context the context information for adding the ser
     * @return a map of file paths to lists of text edits
     */
    Map<String, List<TextEdit>> addModel(AddModelContext context) throws Exception;

    /**
     * Get the list of text edits for the given ser for updating.
     *
     * @param filePath the file path where the ser will be added
     * @param model the ser to be added
     * @return a map of file paths to lists of text edits
     */
    Map<String, List<TextEdit>> updateModel(T model, String filePath);

    /**
     * Get the ser from the source code.
     *
     * @param context the context information for extracting the ser
     * @return the ser extracted from the source code
     */
    T getModelFromSource(ModelFromSourceContext context);

    String kind();
}
