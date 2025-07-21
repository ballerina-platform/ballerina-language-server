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

package io.ballerina.servicemodelgenerator.extension.util;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.ModuleSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.VariableSymbol;
import io.ballerina.compiler.syntax.tree.ExplicitNewExpressionNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.NameReferenceNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;
import io.ballerina.modelgenerator.commons.AnnotationAttachment;
import io.ballerina.modelgenerator.commons.ServiceDatabaseManager;
import io.ballerina.modelgenerator.commons.ServiceDeclaration;
import io.ballerina.modelgenerator.commons.ServiceTypeFunction;
import io.ballerina.projects.Project;
import io.ballerina.servicemodelgenerator.extension.ServiceModelGeneratorConstants;
import io.ballerina.servicemodelgenerator.extension.model.Codedata;
import io.ballerina.servicemodelgenerator.extension.model.Function;
import io.ballerina.servicemodelgenerator.extension.model.FunctionReturnType;
import io.ballerina.servicemodelgenerator.extension.model.MetaData;
import io.ballerina.servicemodelgenerator.extension.model.ModuleAndServiceType;
import io.ballerina.servicemodelgenerator.extension.model.Parameter;
import io.ballerina.servicemodelgenerator.extension.model.PropertyTypeMemberInfo;
import io.ballerina.servicemodelgenerator.extension.model.Service;
import io.ballerina.servicemodelgenerator.extension.model.Value;
import io.ballerina.servicemodelgenerator.extension.service.ServiceBuilderRouter;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static io.ballerina.servicemodelgenerator.extension.util.Utils.getFunctionModel;
import static io.ballerina.servicemodelgenerator.extension.util.Utils.getPath;
import static io.ballerina.servicemodelgenerator.extension.util.Utils.isPresent;
import static io.ballerina.servicemodelgenerator.extension.util.Utils.populateListenerInfo;
import static io.ballerina.servicemodelgenerator.extension.util.Utils.updateAnnotationAttachmentProperty;
import static io.ballerina.servicemodelgenerator.extension.util.Utils.updateValue;

public class ServiceModelUtils {

    /**
     * Update the service model of services other than http.
     *
     * @param serviceModel service model initially populated using the database
     * @param serviceNode service declaration node
     * @param semanticModel semantic model of the source
     */
    public static void updateGenericServiceModel(Service serviceModel, ServiceDeclarationNode serviceNode,
                                                 SemanticModel semanticModel) {
        // handle base path and string literal
        String attachPoint = getPath(serviceNode.absoluteResourcePath());
        if (!attachPoint.isEmpty()) {
            boolean isStringLiteral = attachPoint.startsWith("\"") && attachPoint.endsWith("\"");
            if (isStringLiteral) {
                Value stringLiteralProperty = serviceModel.getStringLiteralProperty();
                if (Objects.nonNull(stringLiteralProperty)) {
                    stringLiteralProperty.setValue(attachPoint);
                } else {
                    serviceModel.setStringLiteral(ServiceModelUtils.getStringLiteralProperty(attachPoint));
                }
            } else {
                Value basePathProperty = serviceModel.getBasePath();
                if (Objects.nonNull(basePathProperty)) {
                    basePathProperty.setValue(attachPoint);
                } else {
                    serviceModel.setBasePath(ServiceModelUtils.getBasePathProperty(attachPoint));
                }
            }
        }

        boolean isGraphql = serviceModel.getModuleName().equals(ServiceModelGeneratorConstants.GRAPHQL);
        List<Function> functionsInSource = serviceNode.members().stream()
                .filter(member -> member instanceof FunctionDefinitionNode)
                .map(member -> getFunctionModel((FunctionDefinitionNode) member, semanticModel, false,
                        isGraphql, Map.of()))
                .toList();

        updateServiceInfoNew(serviceModel, functionsInSource);
        serviceModel.setCodedata(new Codedata(serviceNode.lineRange()));
        populateListenerInfo(serviceModel, serviceNode);
        updateAnnotationAttachmentProperty(serviceNode, serviceModel);

        if (serviceModel.getModuleName().equals(ServiceModelGeneratorConstants.RABBITMQ)) {
            filterRabbitMqFunctions(serviceModel.getFunctions());
        }
    }

    private static void filterRabbitMqFunctions(List<Function> functions) {
        // RabbitMQ services can have on of `onMessage` or `onRequest` functions
        boolean hasOnMessage = false;
        boolean hasOnRequest = false;
        int onMessageIndex = -1;
        int onRequestIndex = -1;
        for (int i = 0; i < functions.size(); i++) {
            Function function = functions.get(i);
            if (function.getName().getValue().equals("onMessage")) {
                hasOnMessage = function.isEnabled();
                onMessageIndex = i;
            } else if (function.getName().getValue().equals("onRequest")) {
                hasOnRequest = function.isEnabled();
                onRequestIndex = i;
            }
        }
        if (hasOnMessage) {
            functions.remove(onRequestIndex);
        } else if (hasOnRequest) {
            functions.remove(onMessageIndex);
        }
    }

    private static void updateServiceInfoNew(Service serviceModel, List<Function> functionsInSource) {
        Utils.populateRequiredFunctions(serviceModel);

        boolean isGraphql = serviceModel.getModuleName().equals(ServiceModelGeneratorConstants.GRAPHQL);

        // mark the enabled functions as true if they present in the source
        serviceModel.getFunctions().forEach(functionModel -> {
            Optional<Function> function = functionsInSource.stream()
                    .filter(newFunction -> isPresent(functionModel, newFunction)
                            && newFunction.getKind().equals(functionModel.getKind()))
                    .findFirst();
            functionModel.setEditable(false);
            function.ifPresentOrElse(
                    func -> updateFunction(functionModel, func, serviceModel),
                    () -> functionModel.setEnabled(false)
            );
        });

        // functions contains in source but not enforced using the service contract type
        functionsInSource.forEach(funcInSource -> {
            if (serviceModel.getFunctions().stream().noneMatch(newFunction -> isPresent(funcInSource, newFunction))) {
                if (isGraphql) {
                    GraphqlUtil.updateGraphqlFunctionMetaData(funcInSource);
                    serviceModel.addFunction(funcInSource);
                } else {
                    serviceModel.addFunction(funcInSource);
                    funcInSource.setEditable(false);
                }
            }
        });
    }

    private static void updateFunction(Function target, Function source, Service service) {
        target.setEnabled(source.isEnabled());
        target.setCodedata(source.getCodedata());
        updateValue(target.getAccessor(), source.getAccessor());
        updateValue(target.getName(), source.getName());

        List<Parameter> sourceParameters = source.getParameters();
        for (Parameter targetParameter: target.getParameters()) {
            AtomicReference<Optional<Parameter>> parameter = new AtomicReference<>(Optional.empty());
            sourceParameters.removeIf(sourceParam -> {
                if (isEqual(targetParameter.getType(), sourceParam.getType())) {
                    parameter.set(Optional.of(sourceParam));
                    return true;
                }
                return false;
            });
            Optional<Parameter> foundSourceParam = parameter.get();
            if (foundSourceParam.isEmpty()) {
                targetParameter.setEnabled(false);
            }
            foundSourceParam.ifPresent(value -> updateParameter(targetParameter, value));
        }
        updateValue(target.getReturnType(), source.getReturnType());
        Value requiredFunctions = service.getProperty(ServiceModelGeneratorConstants.PROPERTY_REQUIRED_FUNCTIONS);
        if (Objects.nonNull(requiredFunctions)) {
            if (source.isEnabled() && requiredFunctions.getItems().contains(source.getName().getValue())) {
                requiredFunctions.setValue(source.getName().getValue());
            }
        }
    }

    private static boolean isEqual(Value target, Value source) {
        return Objects.nonNull(target) && target.getValue().equals(source.getValue());
    }

    private static void updateParameter(Parameter target, Parameter source) {
        target.setEnabled(source.isEnabled());
        target.setKind(source.getKind());
        updateValue(target.getType(), source.getType());
        updateValue(target.getName(), source.getName());
    }

    /**
     * Get the service model for a give service type including its functions.
     *
     * @param moduleName module name
     * @param serviceType service type
     * @return {@link Optional<Service>} service model
     */
    public static Optional<Service> getServiceModelWithFunctions(String moduleName, String serviceType) {
        Optional<Service> serviceOptional = ServiceBuilderRouter.getModelTemplate(moduleName);
        if (serviceOptional.isEmpty() || serviceOptional.get().getPackageName().equals("http")) {
            return serviceOptional;
        }
        Service service = serviceOptional.get();
        int packageId = Integer.parseInt(service.getId());
        ServiceDatabaseManager.getInstance().getMatchingServiceTypeFunctions(packageId, serviceType)
                .forEach(function -> service.getFunctions().add(getFunction(function)));
        service.getServiceType().setValue(serviceType);
        return Optional.of(service);
    }

    public static void populateRequiredFunctionsForServiceType(Service service) {
        int packageId = Integer.parseInt(service.getId());
        String serviceTypeName = Objects.nonNull(service.getServiceType()) ? service.getServiceType().getValue()
                : "Service";
        ServiceDatabaseManager.getInstance().getMatchingServiceTypeFunctions(packageId, serviceTypeName)
                .forEach(function -> service.getFunctions().add(getFunction(function)));
    }

    private static Function getFunction(ServiceTypeFunction function) {

        List<Parameter> parameters = new ArrayList<>();
        for (ServiceTypeFunction.ServiceTypeFunctionParameter parameter : function.parameters()) {
            parameters.add(getParameter(parameter));
        }

        Value.ValueBuilder functionName = new Value.ValueBuilder();
        functionName
                .metadata(function.name(), function.description())
                .setCodedata(new Codedata("FUNCTION_NAME"))
                .value(function.name())
                .valueType("IDENTIFIER")
                .setValueTypeConstraint("string")
                .setPlaceholder(function.name())
                .enabled(true);

        Value.ValueBuilder returnValue = new Value.ValueBuilder();
        returnValue
                .metadata("Return Type", "The return type of the function")
                .value(function.returnType())
                .valueType("TYPE")
                .setPlaceholder(function.returnType())
                .editable(function.returnTypeEditable() == 1)
                .enabled(true)
                .isType(true)
                .optional(true);

        FunctionReturnType functionReturnType = new FunctionReturnType(returnValue.build());
        functionReturnType.setHasError(function.returnError() == 1);

        Function.FunctionBuilder functionBuilder = new Function.FunctionBuilder();
        functionBuilder
                .setMetadata(new MetaData(function.name(), function.description()))
                .kind(function.kind())
                .enabled(function.enable() == 1)
                .editable(true)
                .name(functionName.build())
                .returnType(functionReturnType)
                .parameters(parameters);

        if (function.kind().equals(ServiceModelGeneratorConstants.KIND_RESOURCE)) {
            Value.ValueBuilder accessor = new Value.ValueBuilder()
                    .metadata("Accessor", "The accessor of the resource function")
                    .setCodedata(new Codedata("ACCESSOR"))
                    .value(function.accessor())
                    .valueType("IDENTIFIER")
                    .setValueTypeConstraint("string")
                    .setPlaceholder(function.accessor())
                    .enabled(true)
                    .editable(false)
                    .isType(false)
                    .optional(false)
                    .setAdvanced(false);
            functionBuilder.accessor(accessor.build());
        }

        return functionBuilder.build();
    }

    private static Parameter getParameter(ServiceTypeFunction.ServiceTypeFunctionParameter parameter) {
        Value.ValueBuilder parameterName = new Value.ValueBuilder();
        parameterName
                .setMetadata(new MetaData(parameter.name(), parameter.description()))
                .setCodedata(new Codedata("PARAMETER_NAME"))
                .value(parameter.name())
                .valueType("IDENTIFIER")
                .setPlaceholder(parameter.name())
                .enabled(true)
                .editable(parameter.nameEditable() == 1);

        Value.ValueBuilder parameterType = new Value.ValueBuilder();
        parameterType
                .setMetadata(new MetaData("Type", "The type of the parameter"))
                .value(parameter.type())
                .valueType("TYPE")
                .setPlaceholder(parameter.type())
                .enabled(true)
                .editable(parameter.typeEditable() == 1)
                .isType(true)
                .optional(true);

        Value.ValueBuilder parameterDefaultValue = new Value.ValueBuilder();
        parameterDefaultValue
                .setMetadata(new MetaData("Default Value", "The default value of the parameter"))
                .value(parameter.defaultValue())
                .valueType("EXPRESSION")
                .setPlaceholder(parameter.defaultValue())
                .enabled(true)
                .editable(true)
                .optional(true);

        Parameter.Builder parameterBuilder = new Parameter.Builder();
        parameterBuilder
                .metadata(new MetaData(parameter.name(), parameter.description()))
                .kind(parameter.kind())
                .type(parameterType.build())
                .name(parameterName.build())
                .defaultValue(parameterDefaultValue.build())
                .enabled(true)
                .editable(true)
                .optional(parameter.kind().equals("OPTIONAL"))
                .advanced(false)
                .httpParamType(null);

        return parameterBuilder.build();
    }

    public static Value getTypeDescriptorProperty(ServiceDeclaration template, int packageId) {
        List<String> serviceTypes = ServiceDatabaseManager.getInstance().getServiceTypes(packageId);
        String value = "";
        if (serviceTypes.size() == 1) {
            value = serviceTypes.getFirst();
        }
        List<Object> items = new ArrayList<>();
        items.add("");
        items.addAll(serviceTypes);

        Value.ValueBuilder valueBuilder = new Value.ValueBuilder();
        valueBuilder
                .setMetadata(new MetaData(template.typeDescriptorLabel(), template.typeDescriptorDescription()))
                .setCodedata(new Codedata("SERVICE_TYPE"))
                .value(value)
                .setItems(items)
                .valueType("SINGLE_SELECT")
                .setValueTypeConstraint("string")
                .setPlaceholder(template.typeDescriptorDefaultValue())
                .optional(false)
                .setAdvanced(false)
                .enabled(template.optionalTypeDescriptor() == 0)
                .editable(true)
                .isType(false)
                .setAddNewButton(false);

        return valueBuilder.build();
    }

    public static Value getStringLiteralProperty(String value) {
        Value.ValueBuilder valueBuilder = new Value.ValueBuilder();
        valueBuilder
                .setMetadata(new MetaData("String Literal", "The string literal of the service"))
                .setCodedata(new Codedata("STRING_LITERAL"))
                .value(value)
                .setValues(new ArrayList<>())
                .valueType("SERVICE_PATH")
                .setValueTypeConstraint("string")
                .setPlaceholder("\"/path\"")
                .optional(false)
                .setAdvanced(false)
                .enabled(true)
                .editable(true)
                .isType(false)
                .setAddNewButton(false);

        return valueBuilder.build();
    }

    public static Value getStringLiteral(ServiceDeclaration template) {
        Value.ValueBuilder valueBuilder = new Value.ValueBuilder();
        valueBuilder
                .setMetadata(new MetaData(template.stringLiteralLabel(), template.stringLiteralDescription()))
                .setCodedata(new Codedata("STRING_LITERAL"))
                .value("")
                .setValues(new ArrayList<>())
                .valueType("SERVICE_PATH")
                .setValueTypeConstraint("string")
                .setPlaceholder(template.stringLiteralDefaultValue())
                .optional(false)
                .setAdvanced(false)
                .enabled(true)
                .editable(true)
                .isType(false)
                .setAddNewButton(false);

        return valueBuilder.build();
    }

    public static Value getBasePathProperty(String value) {
        Value.ValueBuilder valueBuilder = new Value.ValueBuilder();
        valueBuilder
                .setMetadata(new MetaData("Base Path", "The base path of the service"))
                .setCodedata(new Codedata("SERVICE_BASE_PATH"))
                .value(value)
                .setValues(new ArrayList<>())
                .valueType("SERVICE_PATH")
                .setValueTypeConstraint("string")
                .setPlaceholder("/")
                .optional(false)
                .setAdvanced(false)
                .enabled(true)
                .editable(true)
                .isType(false)
                .setAddNewButton(false);

        return valueBuilder.build();
    }

    public static Value getBasePathProperty(ServiceDeclaration template) {
        Value.ValueBuilder valueBuilder = new Value.ValueBuilder();
        valueBuilder
                .setMetadata(new MetaData(template.absoluteResourcePathLabel(),
                        template.absoluteResourcePathDescription()))
                .setCodedata(new Codedata("SERVICE_BASE_PATH"))
                .value(template.absoluteResourcePathDefaultValue())
                .setValues(new ArrayList<>())
                .valueType("SERVICE_PATH")
                .setValueTypeConstraint("string")
                .setPlaceholder(template.absoluteResourcePathDefaultValue())
                .optional(false)
                .setAdvanced(false)
                .enabled(true)
                .editable(true)
                .isType(false)
                .setAddNewButton(false);

        return valueBuilder.build();
    }

    public static Value getAnnotationAttachmentProperty(AnnotationAttachment attachment) {
        String typeName = attachment.typeName();
        String[] split = typeName.split(":");
        if (split.length > 1) {
            typeName = split[1];
        }
        PropertyTypeMemberInfo propertyTypeMemberInfo = new PropertyTypeMemberInfo(typeName, attachment.packageInfo(),
                "RECORD_TYPE", true);
        Codedata codedata = new Codedata("ANNOTATION_ATTACHMENT");
        codedata.setOriginalName(attachment.annotName());

        Value.ValueBuilder valueBuilder = new Value.ValueBuilder()
                .setMetadata(new MetaData(attachment.displayName(), attachment.description()))
                .setCodedata(codedata)
                .value("")
                .setValues(new ArrayList<>())
                .valueType("EXPRESSION")
                .setValueTypeConstraint(attachment.typeName())
                .setPlaceholder("{}")
                .optional(true)
                .setAdvanced(true)
                .enabled(true)
                .editable(true)
                .isType(false)
                .setAddNewButton(false)
                .setMembers(List.of(propertyTypeMemberInfo));

        return valueBuilder.build();
    }

    public static Value getListenersProperty(String protocol, String valueType) {
        boolean isMultiple = valueType.equals("MULTIPLE_SELECT");
        MetaData metaData = isMultiple ?
                new MetaData("Listeners", "The Listeners to be bound with the service")
                : new MetaData("Listener", "The Listener to be bound with the service");

        Value.ValueBuilder valueBuilder = new Value.ValueBuilder();
        valueBuilder
                .setMetadata(metaData)
                .setCodedata(new Codedata("LISTENER"))
                .value("")
                .setValues(new ArrayList<>())
                .valueType(valueType)
                .setValueTypeConstraint(protocol + ":" + "Listener")
                .setPlaceholder("")
                .optional(false)
                .setAdvanced(false)
                .enabled(true)
                .editable(true)
                .isType(false)
                .setAddNewButton(isMultiple);

        return valueBuilder.build();
    }

    public static String getProtocol(String moduleName) {
        String[] split = moduleName.split("\\.");
        return split[split.length - 1];
    }


    public static Optional<Service> getHttpService() {
        InputStream resourceStream = ServiceModelUtils.class.getClassLoader()
                .getResourceAsStream("services/http.json");
        if (resourceStream == null) {
            return Optional.empty();
        }

        try (JsonReader reader = new JsonReader(new InputStreamReader(resourceStream, StandardCharsets.UTF_8))) {
            return Optional.of(new Gson().fromJson(reader, Service.class));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    public static void updateListenerItems(String moduleName, SemanticModel semanticModel, Project project,
                                           Service serviceModel) {
        Set<String> listeners = ListenerUtil.getCompatibleListeners(moduleName, semanticModel, project);
        List<String> allValues = serviceModel.getListener().getValues();
        if (Objects.isNull(allValues) || allValues.isEmpty()) {
            listeners.add(serviceModel.getListener().getValue());
        } else {
            listeners.addAll(allValues);
        }
        Value listener = serviceModel.getListener();
        if (!listeners.isEmpty()) {
            listener.setItems(listeners.stream().map(l -> (Object) l).toList());
        }
    }

    public static String serviceTypeWithoutPrefix(String serviceType) {
        String[] serviceTypeNames = serviceType.split(":");
        String actualServiceType = "Service";
        if (serviceTypeNames.length > 1) {
            actualServiceType = serviceTypeNames[1];
        }
        return actualServiceType;
    }

    public static ModuleAndServiceType deriveServiceType(ServiceDeclarationNode serviceNode,
                                                         SemanticModel semanticModel) {
        Optional<TypeDescriptorNode> serviceTypeDesc = serviceNode.typeDescriptor();
        Optional<ModuleSymbol> module = Optional.empty();
        String serviceType = "Service";
        if (serviceTypeDesc.isPresent()) {
            TypeDescriptorNode typeDescriptorNode = serviceTypeDesc.get();
            serviceType = typeDescriptorNode.toString().trim();
            Optional<TypeSymbol> typeSymbol = semanticModel.typeOf(typeDescriptorNode);
            if (typeSymbol.isPresent()) {
                module = typeSymbol.get().getModule();
            }
        }

        if (module.isEmpty()) {
            SeparatedNodeList<ExpressionNode> expressions = serviceNode.expressions();
            if (expressions.isEmpty()) {
                return new ModuleAndServiceType(null, serviceType);
            }
            ExpressionNode expressionNode = expressions.get(0);
            if (expressionNode instanceof ExplicitNewExpressionNode explicitNewExpressionNode) {
                Optional<Symbol> symbol = semanticModel.symbol(explicitNewExpressionNode.typeDescriptor());
                if (symbol.isEmpty()) {
                    return new ModuleAndServiceType(null, serviceType);
                }
                module = symbol.get().getModule();
            } else if (expressionNode instanceof NameReferenceNode nameReferenceNode) {
                Optional<Symbol> symbol = semanticModel.symbol(nameReferenceNode);
                if (symbol.isPresent() && symbol.get() instanceof VariableSymbol variableSymbol) {
                    module = variableSymbol.typeDescriptor().getModule();
                }
            }
        }

        if (module.isEmpty()) {
            return new ModuleAndServiceType(null, serviceType);
        }
        return new ModuleAndServiceType(module.get().getName().orElse(null), serviceType);
    }
}
