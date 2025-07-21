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
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.modelgenerator.commons.AnnotationAttachment;
import io.ballerina.modelgenerator.commons.CommonUtils;
import io.ballerina.modelgenerator.commons.ServiceDatabaseManager;
import io.ballerina.modelgenerator.commons.ServiceDeclaration;
import io.ballerina.servicemodelgenerator.extension.model.AddModelContext;
import io.ballerina.servicemodelgenerator.extension.model.DisplayAnnotation;
import io.ballerina.servicemodelgenerator.extension.model.ModelFromSourceContext;
import io.ballerina.servicemodelgenerator.extension.model.NodeBuilder;
import io.ballerina.servicemodelgenerator.extension.model.Service;
import io.ballerina.servicemodelgenerator.extension.model.Value;
import io.ballerina.servicemodelgenerator.extension.util.ListenerUtil;
import io.ballerina.servicemodelgenerator.extension.util.ServiceModelUtils;
import io.ballerina.servicemodelgenerator.extension.util.Utils;
import org.eclipse.lsp4j.TextEdit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static io.ballerina.servicemodelgenerator.extension.ServiceModelGeneratorConstants.NEW_LINE;
import static io.ballerina.servicemodelgenerator.extension.util.ListenerUtil.getDefaultListenerDeclarationStmt;
import static io.ballerina.servicemodelgenerator.extension.util.ServiceModelUtils.getAnnotationAttachmentProperty;
import static io.ballerina.servicemodelgenerator.extension.util.ServiceModelUtils.getBasePathProperty;
import static io.ballerina.servicemodelgenerator.extension.util.ServiceModelUtils.getListenersProperty;
import static io.ballerina.servicemodelgenerator.extension.util.ServiceModelUtils.getProtocol;
import static io.ballerina.servicemodelgenerator.extension.util.ServiceModelUtils.getStringLiteral;
import static io.ballerina.servicemodelgenerator.extension.util.ServiceModelUtils.getTypeDescriptorProperty;
import static io.ballerina.servicemodelgenerator.extension.util.ServiceModelUtils.populateRequiredFunctionsForServiceType;
import static io.ballerina.servicemodelgenerator.extension.util.ServiceModelUtils.serviceTypeWithoutPrefix;
import static io.ballerina.servicemodelgenerator.extension.util.ServiceModelUtils.updateGenericServiceModel;
import static io.ballerina.servicemodelgenerator.extension.util.ServiceModelUtils.updateListenerItems;
import static io.ballerina.servicemodelgenerator.extension.util.Utils.FunctionAddContext.TRIGGER_ADD;
import static io.ballerina.servicemodelgenerator.extension.util.Utils.getImportStmt;
import static io.ballerina.servicemodelgenerator.extension.util.Utils.getServiceDeclarationNode;
import static io.ballerina.servicemodelgenerator.extension.util.Utils.importExists;
import static io.ballerina.servicemodelgenerator.extension.util.Utils.populateRequiredFuncsDesignApproachAndServiceType;

public class DefaultServiceBuilder implements NodeBuilder<Service> {

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
    public Map<String, List<TextEdit>> addModel(AddModelContext context) throws Exception {
        ListenerUtil.DefaultListener defaultListener = ListenerUtil.getDefaultListener(context);
        List<TextEdit> edits = new ArrayList<>();
        if (Objects.nonNull(defaultListener)) {
            String stmt = getDefaultListenerDeclarationStmt(defaultListener);
            edits.add(new TextEdit(Utils.toRange(defaultListener.linePosition()), stmt));
        }

        Service service = context.service();
        populateRequiredFuncsDesignApproachAndServiceType(service);
        populateRequiredFunctionsForServiceType(service);

        Map<String, String> imports = new HashMap<>();
        String serviceDeclaration = getServiceDeclarationNode(service, TRIGGER_ADD, imports);

        ModulePartNode rootNode = context.document().syntaxTree().rootNode();
        edits.add(new TextEdit(Utils.toRange(rootNode.lineRange().endLine()), NEW_LINE + serviceDeclaration));

        Set<String> importStmts = new HashSet<>();
        if (!importExists(rootNode, service.getOrgName(), service.getModuleName())) {
            importStmts.add(Utils.getImportStmt(service.getOrgName(), service.getModuleName()));
        }
        imports.values().forEach(moduleId -> {
            String[] importParts = moduleId.split("/");
            String orgName = importParts[0];
            String moduleName = importParts[1].split(":")[0];
            if (!importExists(rootNode, orgName, moduleName)) {
                importStmts.add(getImportStmt(orgName, moduleName));
            }
        });

        if (!importStmts.isEmpty()) {
            String importsStmts = String.join(NEW_LINE, importStmts);
            edits.addFirst(new TextEdit(Utils.toRange(rootNode.lineRange().startLine()), importsStmts));
        }

        return Map.of(context.filePath(), edits);
    }

    @Override
    public Map<String, List<TextEdit>> updateModel(Service model, String filePath) {
        return Map.of();
    }

    @Override
    public Service getModelFromSource(ModelFromSourceContext context) {
        if (Objects.isNull(context.moduleName())) {
            return null;
        }
        String serviceType = serviceTypeWithoutPrefix(context.serviceType());
        Optional<Service> service = ServiceModelUtils.getServiceModelWithFunctions(context.moduleName(), serviceType);
        if (service.isEmpty()) {
            return null;
        }
        Service serviceModel = service.get();
        updateGenericServiceModel(serviceModel, (ServiceDeclarationNode) context.node(), context.semanticModel());
        updateListenerItems(context.moduleName(), context.semanticModel(), context.project(), serviceModel);
        return serviceModel;
    }

    @Override
    public String kind() {
        return "default";
    }
}

