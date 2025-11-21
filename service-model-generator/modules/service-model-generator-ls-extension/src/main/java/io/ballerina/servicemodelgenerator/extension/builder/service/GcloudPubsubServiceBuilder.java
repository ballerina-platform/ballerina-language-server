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

package io.ballerina.servicemodelgenerator.extension.builder.service;

import io.ballerina.servicemodelgenerator.extension.model.Codedata;
import io.ballerina.servicemodelgenerator.extension.model.Function;
import io.ballerina.servicemodelgenerator.extension.model.Service;
import io.ballerina.servicemodelgenerator.extension.model.ServiceInitModel;
import io.ballerina.servicemodelgenerator.extension.model.Value;
import io.ballerina.servicemodelgenerator.extension.model.context.AddServiceInitModelContext;
import io.ballerina.servicemodelgenerator.extension.model.context.GetServiceInitModelContext;
import io.ballerina.servicemodelgenerator.extension.model.context.ModelFromSourceContext;
import io.ballerina.servicemodelgenerator.extension.util.ListenerUtil;
import org.eclipse.lsp4j.TextEdit;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.ballerina.servicemodelgenerator.extension.model.ServiceInitModel.KEY_CONFIGURE_LISTENER;
import static io.ballerina.servicemodelgenerator.extension.model.ServiceInitModel.KEY_EXISTING_LISTENER;
import static io.ballerina.servicemodelgenerator.extension.model.ServiceInitModel.KEY_LISTENER_VAR_NAME;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.ARG_TYPE_LISTENER_PARAM_INCLUDED_FIELD;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.CLOSE_BRACE;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.GCLOUD_PUBSUB;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.NEW_LINE;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.ON;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.OPEN_BRACE;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.SERVICE;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.SPACE;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.TWO_NEW_LINES;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.VALUE_TYPE_EXPRESSION;
import static io.ballerina.servicemodelgenerator.extension.util.DatabindUtil.addDataBindingParam;
import static io.ballerina.servicemodelgenerator.extension.util.JmsUtil.ON_MESSAGE_FUNCTION_NAME;
import static io.ballerina.servicemodelgenerator.extension.util.JmsUtil.buildListenerChoice;
import static io.ballerina.servicemodelgenerator.extension.util.JmsUtil.buildServiceCodeEdits;
import static io.ballerina.servicemodelgenerator.extension.util.ServiceModelUtils.getRequiredFunctionsForServiceType;
import static io.ballerina.servicemodelgenerator.extension.util.Utils.FunctionAddContext.TRIGGER_ADD;
import static io.ballerina.servicemodelgenerator.extension.util.Utils.applyEnabledChoiceProperty;
import static io.ballerina.servicemodelgenerator.extension.util.Utils.buildServiceAnnotation;

/**
 * Builder class for Google Cloud Pub/Sub service.
 *
 * @since 1.4.0
 */
public final class GcloudPubsubServiceBuilder extends AbstractServiceBuilder {

    private static final String TYPE_PUBSUB_SERVICE_CONFIG = "pubsub:ServiceConfig";
    private static final String SERVICE_TYPE = "pubsub:Service";
    private static final String PROPERTY_CREDENTIALS = "credentials";
    private static final String PROPERTY_PROJECT_ID = "project";
    private static final String PROPERTY_AUTH = "auth";
    private static final String AUTH_CONFIG_TEMPLATE = "{path: \"%s\"}";
    public static final String PAYLOAD_FIELD_NAME = "data";
    public static final String TYPE_PREFIX = "Message";
    private static final String LABEL_GCLOUD_PUBSUB = "Google Cloud Pub/Sub";

    // Listener configuration property keys
    private static final String[] LISTENER_CONFIG_KEYS = {
            PROPERTY_PROJECT_ID, PROPERTY_CREDENTIALS, KEY_LISTENER_VAR_NAME
    };

    @Override
    public ServiceInitModel getServiceInitModel(GetServiceInitModelContext context) {
        ServiceInitModel serviceInitModel = super.getServiceInitModel(context);
        if (serviceInitModel == null) {
            return null;
        }

        Map<String, Value> properties = serviceInitModel.getProperties();
        Set<String> listeners = ListenerUtil.getCompatibleListeners(context.moduleName(),
                context.semanticModel(), context.project());

        if (!listeners.isEmpty()) {
            Map<String, Value> listenerProps = new LinkedHashMap<>();
            for (String key : LISTENER_CONFIG_KEYS) {
                listenerProps.put(key, properties.remove(key));
            }
            Value choicesProperty = buildListenerChoice(listenerProps, listeners, LABEL_GCLOUD_PUBSUB);
            properties.put(KEY_CONFIGURE_LISTENER, choicesProperty);
        }

        return serviceInitModel;
    }

    @Override
    public Map<String, List<TextEdit>> addServiceInitSource(AddServiceInitModelContext context) {
        ServiceInitModel serviceInitModel = context.serviceInitModel();
        Map<String, Value> properties = serviceInitModel.getProperties();

        applyCredentialsProperty(properties);

        if (!properties.containsKey(KEY_CONFIGURE_LISTENER)) {
            return addServiceWithNewListener(context);
        }

        applyEnabledChoiceProperty(serviceInitModel, KEY_CONFIGURE_LISTENER);

        ListenerDTO listenerDTO;
        applyCredentialsProperty(properties);
        if (properties.containsKey(KEY_EXISTING_LISTENER)) {
            listenerDTO = new ListenerDTO(context.serviceInitModel().getModuleName(),
                    properties.get(KEY_EXISTING_LISTENER).getValue(), "");
        } else {
            listenerDTO = buildListenerDTO(context);
        }

        String serviceCode = buildPubsubServiceCode(serviceInitModel, listenerDTO);
        return buildServiceCodeEdits(context, serviceCode, null);
    }

    private Map<String, List<TextEdit>> addServiceWithNewListener(AddServiceInitModelContext context) {
        ListenerDTO listenerDTO = buildListenerDTO(context);
        String serviceCode = buildPubsubServiceCode(context.serviceInitModel(), listenerDTO);
        return buildServiceCodeEdits(context, serviceCode, null);
    }

    private void applyCredentialsProperty(Map<String, Value> properties) {
        if (!properties.containsKey(PROPERTY_CREDENTIALS)) {
            return;
        }

        Value credentialsValue = properties.get(PROPERTY_CREDENTIALS);
        if (credentialsValue == null || credentialsValue.getValue() == null
                || credentialsValue.getValue().isEmpty()) {
            return;
        }

        String credentialsPath = credentialsValue.getValue();
        String authConfig = String.format(AUTH_CONFIG_TEMPLATE, credentialsPath);

        Value authValue = new Value.ValueBuilder()
                .value(authConfig)
                .valueType(VALUE_TYPE_EXPRESSION)
                .enabled(true)
                .editable(false)
                .setCodedata(new Codedata(null, ARG_TYPE_LISTENER_PARAM_INCLUDED_FIELD))
                .build();
        properties.put(PROPERTY_AUTH, authValue);
        properties.remove(PROPERTY_CREDENTIALS);
    }

    private String buildPubsubServiceCode(ServiceInitModel serviceInitModel, ListenerDTO listenerDTO) {
        Map<String, Value> properties = serviceInitModel.getProperties();

        String serviceAnnotation = buildServiceAnnotation(TYPE_PUBSUB_SERVICE_CONFIG, properties);

        List<Function> functions = getRequiredFunctionsForServiceType(serviceInitModel);
        List<String> functionsStr = AbstractServiceBuilder.buildMethodDefinitions(
                functions, TRIGGER_ADD, new HashMap<>());

        String code = NEW_LINE;
        if (!listenerDTO.listenerDeclaration().isEmpty()) {
            code += listenerDTO.listenerDeclaration() + NEW_LINE;
        }
        code += serviceAnnotation
                + SERVICE + SPACE + SERVICE_TYPE + SPACE
                + ON + SPACE + listenerDTO.listenerVarName() + SPACE
                + OPEN_BRACE
                + NEW_LINE
                + String.join(TWO_NEW_LINES, functionsStr) + NEW_LINE
                + CLOSE_BRACE + NEW_LINE;

        return code;
    }

    @Override
    public Service getModelFromSource(ModelFromSourceContext context) {
        Service service = super.getModelFromSource(context);
        addDataBindingParam(service, ON_MESSAGE_FUNCTION_NAME, context, PAYLOAD_FIELD_NAME, TYPE_PREFIX);
        return service;
    }

    @Override
    public String kind() {
        return GCLOUD_PUBSUB;
    }
}
