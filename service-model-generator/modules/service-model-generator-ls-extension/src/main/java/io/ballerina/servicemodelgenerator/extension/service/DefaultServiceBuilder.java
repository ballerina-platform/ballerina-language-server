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

import io.ballerina.compiler.api.symbols.AnnotationAttachPoint;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.modelgenerator.commons.AnnotationAttachment;
import io.ballerina.modelgenerator.commons.CommonUtils;
import io.ballerina.modelgenerator.commons.ServiceDatabaseManager;
import io.ballerina.modelgenerator.commons.ServiceDeclaration;
import io.ballerina.servicemodelgenerator.extension.ServiceModelGeneratorService;
import io.ballerina.servicemodelgenerator.extension.model.DisplayAnnotation;
import io.ballerina.servicemodelgenerator.extension.model.ModelFromSourceContext;
import io.ballerina.servicemodelgenerator.extension.model.NodeBuilder;
import io.ballerina.servicemodelgenerator.extension.model.Service;
import io.ballerina.servicemodelgenerator.extension.model.Value;
import io.ballerina.servicemodelgenerator.extension.util.ServiceModelUtils;
import org.eclipse.lsp4j.TextEdit;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static io.ballerina.servicemodelgenerator.extension.util.ServiceModelUtils.getAnnotationAttachmentProperty;
import static io.ballerina.servicemodelgenerator.extension.util.ServiceModelUtils.getBasePathProperty;
import static io.ballerina.servicemodelgenerator.extension.util.ServiceModelUtils.getListenersProperty;
import static io.ballerina.servicemodelgenerator.extension.util.ServiceModelUtils.getProtocol;
import static io.ballerina.servicemodelgenerator.extension.util.ServiceModelUtils.getStringLiteral;
import static io.ballerina.servicemodelgenerator.extension.util.ServiceModelUtils.getTypeDescriptorProperty;
import static io.ballerina.servicemodelgenerator.extension.util.ServiceModelUtils.serviceTypeWithoutPrefix;
import static io.ballerina.servicemodelgenerator.extension.util.ServiceModelUtils.updateGenericServiceModel;
import static io.ballerina.servicemodelgenerator.extension.util.ServiceModelUtils.updateListenerItems;

public class DefaultServiceBuilder implements NodeBuilder<Service> {

    private ServiceModelGeneratorService.ModuleAndServiceType moduleAndServiceType;

    @Override
    public Optional<Service> getModelTemplate(String moduleName) {
        Optional<ServiceDeclaration> serviceDeclaration = ServiceDatabaseManager.getInstance()
                .getServiceDeclaration(moduleName);
        if (serviceDeclaration.isEmpty()) {
            return Optional.empty();
        }
        ServiceDeclaration serviceTemplate = serviceDeclaration.get();
        ServiceDeclaration.Package pkg = serviceTemplate.packageInfo();

        String protocol = getProtocol(moduleName);

        String label = serviceTemplate.displayName();
        String documentation = "Add the service documentation";
        String icon = CommonUtils.generateIcon(pkg.org(), pkg.name(), pkg.version());

        Map<String, Value> properties = new LinkedHashMap<>();

        Service.ServiceModelBuilder serviceBuilder = new Service.ServiceModelBuilder();
        serviceBuilder
                .setId(String.valueOf(pkg.packageId()))
                .setName(label)
                .setType(moduleName)
                .setDisplayName(label)
                .setDescription(documentation)
                .setDisplayAnnotation(new DisplayAnnotation(label, icon))
                .setModuleName(moduleName)
                .setOrgName(pkg.org())
                .setVersion(pkg.version())
                .setPackageName(pkg.name())
                .setListenerProtocol(protocol)
                .setIcon(icon)
                .setProperties(properties)
                .setFunctions(new ArrayList<>());

        Service service = serviceBuilder.build();
        properties.put("listener", getListenersProperty(protocol, serviceTemplate.listenerKind()));

        // type descriptor
        properties.put("serviceType", getTypeDescriptorProperty(serviceTemplate, pkg.packageId()));

        // base path
        if (serviceTemplate.optionalAbsoluteResourcePath() == 0) {
            properties.put("basePath", getBasePathProperty(serviceTemplate));
        }

        // string literal
        if (serviceTemplate.optionalStringLiteral() == 0) {
            properties.put("stringLiteral", getStringLiteral(serviceTemplate));
        }

        List<AnnotationAttachment> annotationAttachments = ServiceDatabaseManager.getInstance()
                .getAnnotationAttachments(pkg.packageId());
        for (AnnotationAttachment annotationAttachment : annotationAttachments) {
            if (annotationAttachment.attachmentPoints().contains(AnnotationAttachPoint.SERVICE)) {
                String key = "annot" + annotationAttachment.annotName();
                properties.put(key, getAnnotationAttachmentProperty(annotationAttachment));
            }
        }

        return Optional.of(service);
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
        if (Objects.isNull(moduleAndServiceType.moduleName())) {
            return null;
        }
        String serviceType = serviceTypeWithoutPrefix(moduleAndServiceType);
        Optional<Service> service = ServiceModelUtils.getServiceModelWithFunctions("moduleName", serviceType);
        if (service.isEmpty()) {
            return null;
        }
        Service serviceModel = service.get();
        updateGenericServiceModel(serviceModel, (ServiceDeclarationNode) context.node(), context.semanticModel());
        updateListenerItems("moduleName", context.semanticModel(), context.project(), serviceModel);
        return null;
    }

    @Override
    public String kind() {
        return "default";
    }

    public record ModuleAndServiceType(String moduleName, String serviceType) {
    }
}

