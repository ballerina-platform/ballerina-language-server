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

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.projects.Document;
import io.ballerina.projects.Project;
import io.ballerina.servicemodelgenerator.extension.model.AddModelContext;
import io.ballerina.servicemodelgenerator.extension.model.ModelFromSourceContext;
import io.ballerina.servicemodelgenerator.extension.model.ModuleAndServiceType;
import io.ballerina.servicemodelgenerator.extension.model.NodeBuilder;
import io.ballerina.servicemodelgenerator.extension.model.Service;
import io.ballerina.servicemodelgenerator.extension.model.UpdateModelContext;
import io.ballerina.servicemodelgenerator.extension.util.ServiceModelUtils;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;
import org.eclipse.lsp4j.TextEdit;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import static io.ballerina.servicemodelgenerator.extension.util.Constants.AI;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.HTTP;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.TCP;

public class ServiceBuilderRouter {

    private static final Map<String, Supplier<? extends NodeBuilder<Service>>> CONSTRUCTOR_MAP = new HashMap<>() {{
        put(HTTP, HttpServiceBuilder::new);
        put(AI, AiChatServiceBuilder::new);
        put(TCP, TCPServiceBuilder::new);
    }};

    public static NodeBuilder<?> getServiceBuilder(String protocol) {
        return CONSTRUCTOR_MAP.getOrDefault(protocol, DefaultServiceBuilder::new).get();
    }

    public static Optional<Service> getModelTemplate(String moduleName) {
        NodeBuilder<?> serviceBuilder = getServiceBuilder(moduleName);
        Optional<?> modelTemplate = serviceBuilder.getModelTemplate(moduleName);
        if (modelTemplate.isEmpty() || !(modelTemplate.get() instanceof Service)) {
            return Optional.empty();
        }
        return Optional.of((Service) modelTemplate.get());
    }

    public static Service getServiceFromSource(Node node, Project project,
                                               SemanticModel semanticModel,
                                               WorkspaceManager workspaceManager) {
        ModuleAndServiceType moduleAndServiceType = ServiceModelUtils.deriveServiceType(
                (ServiceDeclarationNode) node, semanticModel);
        if (Objects.isNull(moduleAndServiceType.moduleName())) {
            return null;
        }
        NodeBuilder<?> serviceBuilder = getServiceBuilder(moduleAndServiceType.moduleName());
        ModelFromSourceContext context = new ModelFromSourceContext(node, project, semanticModel,
                workspaceManager, moduleAndServiceType.moduleName(), moduleAndServiceType.serviceType());
        return (Service) serviceBuilder.getModelFromSource(context);
    }

    public static Map<String, List<TextEdit>> addService(Service service,
                                                         SemanticModel semanticModel, Project project,
                                                         WorkspaceManager workspaceManager,
                                                         String filePath, Document document) throws Exception {
        NodeBuilder<?> serviceBuilder = getServiceBuilder(service.getModuleName());
        AddModelContext context = new AddModelContext(service, null, semanticModel, project,
                workspaceManager, filePath, document);
        return serviceBuilder.addModel(context);
    }

    public static Map<String, List<TextEdit>> updateService(Service service,
                                                            SemanticModel semanticModel,
                                                            WorkspaceManager workspaceManager,
                                                            String filePath, Document document,
                                                            ServiceDeclarationNode serviceNode) throws Exception {
        NodeBuilder<?> serviceBuilder = getServiceBuilder(service.getModuleName());
        UpdateModelContext context = new UpdateModelContext(service, null, semanticModel, null,
                workspaceManager, filePath, document, serviceNode, null);
        return serviceBuilder.updateModel(context);
    }
}
