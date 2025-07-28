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

import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.modelgenerator.commons.ServiceDatabaseManager;
import io.ballerina.servicemodelgenerator.extension.model.Codedata;
import io.ballerina.servicemodelgenerator.extension.model.Function;
import io.ballerina.servicemodelgenerator.extension.model.ModelFromSourceContext;
import io.ballerina.servicemodelgenerator.extension.model.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static io.ballerina.servicemodelgenerator.extension.function.GraphqlFunctionBuilder.getGraphqlFunctionModel;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.GRAPHQL;
import static io.ballerina.servicemodelgenerator.extension.util.ServiceModelUtils.getFunction;
import static io.ballerina.servicemodelgenerator.extension.util.ServiceModelUtils.serviceTypeWithoutPrefix;
import static io.ballerina.servicemodelgenerator.extension.util.ServiceModelUtils.updateListenerItems;
import static io.ballerina.servicemodelgenerator.extension.util.ServiceModelUtils.updateServiceInfoNew;
import static io.ballerina.servicemodelgenerator.extension.util.Utils.populateListenerInfo;
import static io.ballerina.servicemodelgenerator.extension.util.Utils.updateAnnotationAttachmentProperty;

public class GraphqlServiceBuilder extends AbstractServiceBuilder {
    @Override
    public Service getModelFromSource(ModelFromSourceContext context) {
        if (Objects.isNull(context.moduleName())) {
            return null;
        }
        String serviceType = serviceTypeWithoutPrefix(context.serviceType());
        Optional<Service> service = ServiceBuilderRouter.getModelTemplate(context.orgName(), context.moduleName());
        if (service.isEmpty()) {
            return null;
        }
        Service serviceModel = service.get();
        int packageId = Integer.parseInt(serviceModel.getId());
        ServiceDatabaseManager.getInstance().getMatchingServiceTypeFunctions(packageId, serviceType)
                .forEach(function -> serviceModel.getFunctions().add(getFunction(function)));
        serviceModel.getServiceType().setValue(serviceType);

        ServiceDeclarationNode serviceNode = (ServiceDeclarationNode) context.node();
        extractServicePathInfo(serviceNode, serviceModel);

        List<Function> functionsInSource = serviceNode.members().stream()
                .filter(member -> member instanceof FunctionDefinitionNode)
                .map(member -> getGraphqlFunctionModel((FunctionDefinitionNode) member, Map.of()))
                .toList();

        updateServiceInfoNew(serviceModel, functionsInSource);
        serviceModel.setCodedata(new Codedata(serviceNode.lineRange()));
        populateListenerInfo(serviceModel, serviceNode);
        updateAnnotationAttachmentProperty(serviceNode, serviceModel);
        updateListenerItems(context.moduleName(), context.semanticModel(), context.project(), serviceModel);
        return serviceModel;
    }

    @Override
    public String kind() {
        return GRAPHQL;
    }
}
