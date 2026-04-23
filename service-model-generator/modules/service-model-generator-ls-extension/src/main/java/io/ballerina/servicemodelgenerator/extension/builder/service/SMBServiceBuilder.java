/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com)
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

package io.ballerina.servicemodelgenerator.extension.builder.service;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.openapi.core.generators.common.exception.BallerinaOpenApiException;
import io.ballerina.projects.Document;
import io.ballerina.servicemodelgenerator.extension.model.Codedata;
import io.ballerina.servicemodelgenerator.extension.model.Function;
import io.ballerina.servicemodelgenerator.extension.model.MetaData;
import io.ballerina.servicemodelgenerator.extension.model.Parameter;
import io.ballerina.servicemodelgenerator.extension.model.Service;
import io.ballerina.servicemodelgenerator.extension.model.ServiceInitModel;
import io.ballerina.servicemodelgenerator.extension.model.Value;
import io.ballerina.servicemodelgenerator.extension.model.context.AddServiceInitModelContext;
import io.ballerina.servicemodelgenerator.extension.model.context.GetModelContext;
import io.ballerina.servicemodelgenerator.extension.model.context.GetServiceInitModelContext;
import io.ballerina.servicemodelgenerator.extension.model.context.ModelFromSourceContext;
import io.ballerina.servicemodelgenerator.extension.util.ListenerUtil;
import io.ballerina.servicemodelgenerator.extension.util.Utils;
import org.ballerinalang.formatter.core.FormatterException;
import org.ballerinalang.langserver.commons.eventsync.exceptions.EventSyncException;
import org.ballerinalang.langserver.commons.workspace.WorkspaceDocumentException;
import org.eclipse.lsp4j.TextEdit;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.ballerina.servicemodelgenerator.extension.model.ServiceInitModel.KEY_CONFIGURE_LISTENER;
import static io.ballerina.servicemodelgenerator.extension.model.ServiceInitModel.KEY_LISTENER_VAR_NAME;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.BALLERINA;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.CLOSE_BRACE;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.NEW_LINE;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.ON;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.OPEN_BRACE;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.PROP_READONLY_METADATA_KEY;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.SERVICE;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.SMB;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.SPACE;
import static io.ballerina.servicemodelgenerator.extension.util.ServiceModelUtils.extractFunctionsFromSource;
import static io.ballerina.servicemodelgenerator.extension.util.ServiceModelUtils.getReadonlyMetadata;
import static io.ballerina.servicemodelgenerator.extension.util.ServiceModelUtils.updateListenerItems;
import static io.ballerina.servicemodelgenerator.extension.util.Utils.applyEnabledChoiceProperty;
import static io.ballerina.servicemodelgenerator.extension.util.Utils.getImportStmt;
import static io.ballerina.servicemodelgenerator.extension.util.Utils.importExists;
import static io.ballerina.servicemodelgenerator.extension.util.Utils.populateListenerInfo;
import static io.ballerina.servicemodelgenerator.extension.util.Utils.updateAnnotationAttachmentProperty;
import static io.ballerina.servicemodelgenerator.extension.util.Utils.updateServiceDocs;

/**
 * Builder class for SMB service.
 *
 * @since 1.5.0
 */
public class SMBServiceBuilder extends AbstractServiceBuilder {

    private static final String SMB_INIT_JSON = "services/smb_init.json";
    private static final String SMB_SERVICE_JSON = "services/smb_service.json";

    // Display label
    private static final String LABEL_SMB = "SMB";

    // Listener configuration property keys
    private static final List<String> LISTENER_CONFIG_KEYS = List.of(
            KEY_LISTENER_VAR_NAME, "host", "portNumber", "share", "authentication"
    );
    public static final String DATA_BINDING = "DATA_BINDING";
    public static final String STREAM = "stream";
    public static final String EVENT = "EVENT";

    @Override
    public String kind() {
        return "SMB";
    }

    @Override
    public ServiceInitModel getServiceInitModel(GetServiceInitModelContext context) {
        InputStream resourceStream = HttpServiceBuilder.class.getClassLoader()
                .getResourceAsStream(SMB_INIT_JSON);
        if (resourceStream == null) {
            return null;
        }

        try (JsonReader reader = new JsonReader(new InputStreamReader(resourceStream, StandardCharsets.UTF_8))) {
            ServiceInitModel serviceInitModel = new Gson().fromJson(reader, ServiceInitModel.class);
            Value listenerNameProp = listenerNameProperty(context);
            Value listener = serviceInitModel.getProperties().get(KEY_LISTENER_VAR_NAME);
            listener.setValue(listenerNameProp.getValue());

            // Check for existing compatible SMB listeners
            // SMB is new - no legacy filtering required
            java.util.Set<String> compatibleListeners = ListenerUtil.getCompatibleListeners(context.moduleName(),
                    context.semanticModel(), context.project());

            if (!compatibleListeners.isEmpty()) {
                Map<String, Value> properties = serviceInitModel.getProperties();
                Map<String, Value> listenerProps =
                        ListenerUtil.removeAndCollectListenerProperties(properties, LISTENER_CONFIG_KEYS);
                Value choicesProperty = ListenerUtil.buildListenerChoiceProperty(listenerProps, compatibleListeners,
                        LABEL_SMB);
                properties.put(KEY_CONFIGURE_LISTENER, choicesProperty);
            }

            return serviceInitModel;
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public Map<String, List<TextEdit>> addServiceInitSource(AddServiceInitModelContext context)
            throws WorkspaceDocumentException, FormatterException, IOException, BallerinaOpenApiException,
            EventSyncException {
        ServiceInitModel serviceInitModel = context.serviceInitModel();

        Map<String, Value> properties = serviceInitModel.getProperties();

        // Check if listener choice property exists and apply it first
        if (properties.containsKey(KEY_CONFIGURE_LISTENER)) {
            applyEnabledChoiceProperty(serviceInitModel, KEY_CONFIGURE_LISTENER);
        }

        properties = serviceInitModel.getProperties();

        // Check if we should use an existing listener
        boolean useExistingListener = ListenerUtil.shouldUseExistingListener(properties);
        String listenerVarName;
        String listenerDeclaration = "";
        String folderPath = getPropertyValueLiteralValue(properties, "path", "\"/\"");

        if (useExistingListener) {
            listenerVarName = ListenerUtil.getExistingListenerName(properties).orElse("");
        } else {
            listenerVarName = properties.get("listenerVarName").getValue();
            String host = getPropertyValueLiteralValue(properties, "host", "\"localhost\"");
            String port = getPropertyValue(properties, "portNumber", "445");
            String share = getPropertyValueLiteralValue(properties, "share", "\"\"");

            applyEnabledChoiceProperty(serviceInitModel, "authentication");
            properties = serviceInitModel.getProperties();
            String username = getPropertyValue(properties, "userName", "");
            String password = getPropertyValue(properties, "password", "");
            String domain = getPropertyValue(properties, "domain", "");

            // Build the listener declaration
            StringBuilder listenerBuilder = new StringBuilder();
            listenerBuilder.append("listener smb:Listener ").append(listenerVarName).append(" = new(");
            listenerBuilder.append("host = ").append(host).append(", ");
            listenerBuilder.append("port = ").append(port).append(", ");
            listenerBuilder.append("share = ").append(share);

            // Add authentication configuration if any auth details are provided
            if (!username.isEmpty() || !password.isEmpty()) {
                listenerBuilder.append(", auth = { credentials: { ");
                if (!username.isEmpty()) {
                    listenerBuilder.append("username: ").append(username);
                    if (!password.isEmpty() || !domain.isEmpty()) {
                        listenerBuilder.append(", ");
                    }
                }
                if (!password.isEmpty()) {
                    listenerBuilder.append("password: ").append(password);
                    if (!domain.isEmpty()) {
                        listenerBuilder.append(", ");
                    }
                }
                if (!domain.isEmpty()) {
                    listenerBuilder.append("domain: ").append(domain);
                }
                listenerBuilder.append(" } }");
            }

            listenerBuilder.append(");");
            listenerDeclaration = listenerBuilder.toString();
        }

        Document document = context.document();
        io.ballerina.compiler.syntax.tree.ModulePartNode modulePartNode =
                document.syntaxTree().rootNode();

        // Service-level annotation for monitoring path
        String serviceConfigAnnotation = "@smb:ServiceConfig {" + NEW_LINE +
                "    path: " + folderPath + NEW_LINE +
                "}" + NEW_LINE;

        String serviceCode;
        if (useExistingListener) {
            serviceCode = NEW_LINE +
                    serviceConfigAnnotation +
                    SERVICE + SPACE + ON + SPACE + listenerVarName + SPACE + OPEN_BRACE +
                    NEW_LINE +
                    CLOSE_BRACE + NEW_LINE;
        } else {
            serviceCode = NEW_LINE +
                    listenerDeclaration +
                    NEW_LINE +
                    NEW_LINE +
                    serviceConfigAnnotation +
                    SERVICE + SPACE + ON + SPACE + listenerVarName + SPACE + OPEN_BRACE +
                    NEW_LINE +
                    CLOSE_BRACE + NEW_LINE;
        }

        List<TextEdit> edits = new ArrayList<>();
        if (!importExists(modulePartNode, serviceInitModel.getOrgName(), serviceInitModel.getModuleName())) {
            String importText = getImportStmt(serviceInitModel.getOrgName(), serviceInitModel.getModuleName());
            edits.add(new TextEdit(Utils.toRange(modulePartNode.lineRange().startLine()), importText));
        }
        edits.add(new TextEdit(Utils.toRange(modulePartNode.lineRange().endLine()), serviceCode));

        return Map.of(context.filePath(), edits);
    }

    @Override
    public Service getModelFromSource(ModelFromSourceContext context) {
        Optional<Service> service = getModelTemplate(GetModelContext.fromServiceAndFunctionType(BALLERINA, SMB));
        if (service.isEmpty()) {
            return null;
        }

        Service serviceModel = service.get();
        ServiceDeclarationNode serviceNode = (ServiceDeclarationNode) context.node();
        SemanticModel semanticModel = context.semanticModel();
        Codedata codedata = new Codedata.Builder()
                .setLineRange(serviceNode.lineRange())
                .setOrgName(context.orgName())
                .setPackageName(context.packageName())
                .setModuleName(context.moduleName())
                .build();
        serviceModel.setCodedata(codedata);

        List<Function> functionsInSource = extractFunctionsFromSource(serviceNode);
        Map<String, Function> functionMap = new HashMap<>();
        for (Function function : serviceModel.getFunctions()) {
            if (function.getName() != null && function.getName().getValue() != null) {
                functionMap.put(function.getName().getValue(), function);
            }
        }

        if (serviceModel.getFunctions() != null) {
            for (Function sourceFunc : functionsInSource) {
                if (sourceFunc.isEnabled() && sourceFunc.getName() != null) {
                    String sourceFuncName = sourceFunc.getName().getValue();
                    Function modelFunc = functionMap.get(sourceFuncName);
                    if (modelFunc != null) {
                        modelFunc.setEnabled(true);
                        modelFunc.setCodedata(sourceFunc.getCodedata());
                        modelFunc.getCodedata().setModuleName(SMB);

                        enableParameters(sourceFunc, modelFunc);
                        updateDatabindingParameter(sourceFunc, modelFunc);

                        if (modelFunc.getProperties().containsKey(STREAM)) {
                            setStreamProperty(modelFunc, sourceFunc);
                        }
                    } else {
                        sourceFunc.setEnabled(true);
                        sourceFunc.setOptional(true);
                        sourceFunc.setEditable(false);
                        MetaData sourceMetadata = sourceFunc.getMetadata();
                        String description = sourceMetadata != null ? sourceMetadata.description() : null;
                        sourceFunc.setMetadata(new MetaData(EVENT, description));
                        serviceModel.addFunction(sourceFunc);
                    }
                }
            }
        }

        if (serviceModel.getProperty(PROP_READONLY_METADATA_KEY) == null) {
            String serviceType = serviceModel.getType();
            Value readOnlyMetadata = getReadonlyMetadata(serviceModel.getOrgName(), serviceModel.getPackageName(),
                    serviceType);
            serviceModel.getProperties().put(PROP_READONLY_METADATA_KEY, readOnlyMetadata);
        }

        updateReadOnlyMetadataWithAnnotations(serviceModel, serviceNode, context);
        populateListenerInfo(serviceModel, serviceNode);
        updateServiceDocs(serviceNode, serviceModel);

        // SMB is a new module - always keep annotServiceConfig (no legacy support to handle)
        updateAnnotationAttachmentProperty(serviceNode, serviceModel);

        updateListenerItems(SMB, semanticModel, context.project(), serviceModel);
        return serviceModel;
    }

    @Override
    public Optional<Service> getModelTemplate(GetModelContext context) {
        InputStream resourceStream = HttpServiceBuilder.class.getClassLoader()
                .getResourceAsStream(SMB_SERVICE_JSON);
        if (resourceStream == null) {
            return Optional.empty();
        }

        try (JsonReader reader = new JsonReader(new InputStreamReader(resourceStream, StandardCharsets.UTF_8))) {
            Service service = new Gson().fromJson(reader, Service.class);
            return Optional.of(service);
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    /**
     * Helper method to get property value with default fallback.
     */
    private String getPropertyValue(Map<String, Value> properties, String key, String defaultValue) {
        Value property = properties.get(key);
        if (property != null && property.getValue() != null && !property.getValue().isEmpty()) {
            return property.getValue();
        }
        return defaultValue;
    }

    /**
     * Helper method to get property literal value with default fallback.
     */
    private String getPropertyValueLiteralValue(Map<String, Value> properties, String key, String defaultValue) {
        Value property = properties.get(key);
        if (property != null && property.getLiteralValue() != null && !property.getLiteralValue().isEmpty()) {
            return property.getLiteralValue();
        }
        return defaultValue;
    }

    /**
     * Sets the stream property based on the first parameter type of the function.
     * Stream property is set to true if the first parameter has a type of stream<{type},error>.
     *
     * @param modelFunc  The model function to update
     * @param sourceFunc The source function to check
     */
    private void setStreamProperty(Function modelFunc, Function sourceFunc) {
        boolean isStream = false;

        if (sourceFunc.getParameters() != null && !sourceFunc.getParameters().isEmpty()) {
            Parameter firstParam = sourceFunc.getParameters().get(0);
            if (firstParam.getType() != null) {
                String paramType = firstParam.getType().getValue();
                if (paramType != null && paramType.startsWith("stream<")) {
                    isStream = true;
                }
            }
        }

        Value streamProperty = new Value.ValueBuilder()
                .value(String.valueOf(isStream))
                .enabled(isStream)
                .editable(false)
                .optional(false)
                .setAdvanced(false)
                .build();

        modelFunc.addProperty("stream", streamProperty);
    }

    private static void enableParameters(Function sourceFunc, Function modelFunc) {
        modelFunc.getParameters().forEach(
                parameter -> parameter.setEnabled(false)
        );
        for (Parameter sourceParam : sourceFunc.getParameters()) {
            modelFunc.getParameters().stream().filter(
                    modelParam -> modelParam.getType().getValue()
                            .equals(sourceParam.getType().getValue()) ||
                            modelParam.getKind().equals(DATA_BINDING)
                            || modelParam.getName().getValue().equals("content")
            ).forEach(
                    modelParam -> modelParam.setEnabled(true)
            );
        }
    }

    private static void updateDatabindingParameter(Function sourceFunc, Function modelFunc) {
        if (sourceFunc.getParameters() != null) {
            Parameter sourceParam = sourceFunc.getParameters().getFirst();
            if (modelFunc.getParameters() != null) {
                Parameter modelParam = modelFunc.getParameters().getFirst();

                if (modelParam.getType() != null &&
                        DATA_BINDING.equals(modelParam.getKind())) {
                    if (sourceParam.getName() != null && modelParam.getName() != null) {
                        modelParam.getName().setValue(sourceParam.getName().getValue());
                    }
                    if (sourceParam.getType() != null && modelParam.getType() != null) {
                        modelParam.getType().setValue(sourceParam.getType().getValue());
                    }
                }
            }
        }
    }

}
