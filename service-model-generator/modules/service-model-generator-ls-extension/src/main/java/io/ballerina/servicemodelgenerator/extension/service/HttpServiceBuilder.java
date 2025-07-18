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

package io.ballerina.servicemodelgenerator.extension.service;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;
import io.ballerina.projects.Document;
import io.ballerina.servicemodelgenerator.extension.model.ModelFromSourceContext;
import io.ballerina.servicemodelgenerator.extension.model.Service;
import org.eclipse.lsp4j.TextEdit;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.ballerina.compiler.syntax.tree.SyntaxKind.OBJECT_TYPE_DESC;
import static io.ballerina.servicemodelgenerator.extension.util.HttpUtil.updateHttpServiceContractModel;
import static io.ballerina.servicemodelgenerator.extension.util.HttpUtil.updateHttpServiceModel;
import static io.ballerina.servicemodelgenerator.extension.util.ServiceModelUtils.updateListenerItems;
import static io.ballerina.servicemodelgenerator.extension.util.Utils.getHttpServiceContractSym;

public final class HttpServiceBuilder extends DefaultServiceBuilder {

    private static final String PROTOCOL = "http";
    private static final String HTTP_SERVICE_MODEL_LOCATION = "services/http.json";

    public HttpServiceBuilder() {
    }

    @Override
    public Optional<Service> getModelTemplate(String moduleName) {
        InputStream resourceStream = HttpServiceBuilder.class.getClassLoader()
                .getResourceAsStream(HTTP_SERVICE_MODEL_LOCATION);
        if (resourceStream == null) {
            return Optional.empty();
        }

        try (JsonReader reader = new JsonReader(new InputStreamReader(resourceStream, StandardCharsets.UTF_8))) {
            return new Gson().fromJson(reader, Service.class);
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    @Override
    public Map<String, List<TextEdit>> addModel(Service model, String filePath) {
        return Map.of();
    }

    @Override
    public Map<String, List<TextEdit>> updateModel(Service model, String filePath) {
        return Map.of();
    }

    @Override
    public Service getModelFromSource(ModelFromSourceContext context) {
        Optional<Service> service = getModelTemplate(PROTOCOL);
        if (service.isEmpty()) {
            return null;
        }
        Service serviceModel = service.get();
        serviceModel.setFunctions(new ArrayList<>());
        boolean serviceContractExists = false;
        ServiceDeclarationNode serviceNode = (ServiceDeclarationNode) context.node();
        SemanticModel semanticModel = context.semanticModel();
        if (serviceNode.typeDescriptor().isPresent()) {
            Optional<Symbol> httpServiceContractSym = getHttpServiceContractSym(semanticModel,
                    serviceNode.typeDescriptor().get());
            if (httpServiceContractSym.isPresent() && httpServiceContractSym.get().getLocation().isPresent()) {
                Path contractPath = context.project().sourceRoot().toAbsolutePath()
                        .resolve(httpServiceContractSym.get().getLocation().get().lineRange().fileName());
                Optional<Document> contractDoc = context.workspaceManager().document(contractPath);
                if (contractDoc.isPresent()) {
                    ModulePartNode contractModulePartNode = contractDoc.get().syntaxTree().rootNode();
                    Optional<TypeDefinitionNode> serviceContractType = contractModulePartNode.members().stream()
                            .filter(member -> member.kind().equals(SyntaxKind.TYPE_DEFINITION))
                            .map(member -> ((TypeDefinitionNode) member))
                            .filter(member -> member.typeDescriptor().kind().equals(OBJECT_TYPE_DESC))
                            .findFirst();
                    if (serviceContractType.isPresent()) {
                        serviceContractExists = true;
                        updateHttpServiceContractModel(serviceModel, serviceContractType.get(), serviceNode);
                    }
                }
            }
        }

        if (!serviceContractExists) {
            updateHttpServiceModel(serviceModel, serviceNode);
        }

        updateListenerItems(PROTOCOL, semanticModel, context.project(), serviceModel);
        return super.getModelFromSource(context);
    }

    @Override
    public String kind() {
        return PROTOCOL;
    }
}
