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

import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.openapi.core.generators.common.exception.BallerinaOpenApiException;
import io.ballerina.servicemodelgenerator.extension.model.ServiceInitModel;
import io.ballerina.servicemodelgenerator.extension.model.Value;
import io.ballerina.servicemodelgenerator.extension.model.context.AddServiceInitModelContext;
import io.ballerina.servicemodelgenerator.extension.util.Utils;
import org.ballerinalang.formatter.core.FormatterException;
import org.ballerinalang.langserver.commons.eventsync.exceptions.EventSyncException;
import org.ballerinalang.langserver.commons.workspace.WorkspaceDocumentException;
import org.eclipse.lsp4j.TextEdit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.ballerina.servicemodelgenerator.extension.model.ServiceInitModel.KEY_LISTENER_VAR_NAME;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.CLOSE_BRACE;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.GOOGLE_CHAT;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.NEW_LINE;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.ON;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.OPEN_BRACE;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.SERVICE;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.SPACE;
import static io.ballerina.servicemodelgenerator.extension.util.Utils.getImportStmt;
import static io.ballerina.servicemodelgenerator.extension.util.Utils.importExists;

/**
 * Builder for the Google Chat event-integration service. Generates a {@code chat:Listener} plus an
 * empty {@code chat:ChatService} on it, annotated with the required {@code @chat:ServiceConfig}; the
 * individual interaction handlers (onMessage, onAddedToSpace, ...) are added on demand through the
 * "Add Handler" panel, mirroring the WhatsApp/Telegram event-integration UX.
 *
 * <p>{@code ballerinax/googleapis.chat}'s default module prefix is {@code chat} (the last segment of
 * the dotted module name), matching the connector's own documented usage — unlike WhatsApp Business,
 * no import alias is needed.
 *
 * <p>The connector's {@code ListenerConfig.auth} is a 3-way union (service account / OAuth2 / bearer
 * token) and its required {@code @ServiceConfig} annotation supports 2 verification modes (endpoint
 * URL / project number). This builder only supports the most common combination — service-account
 * file auth plus HTTP endpoint URL verification — matching WhatsApp/Telegram's flat-field simplicity.
 * Users who need OAuth2, bearer-token, or project-number verification can hand-edit the generated
 * code afterward.
 *
 * @since 1.8.0
 */
public class GoogleChatServiceBuilder extends AbstractServiceBuilder {

    private static final String ORG = "ballerinax";
    private static final String MODULE_NAME = "googleapis.chat";
    private static final String LISTENER_TYPE = "chat:Listener";
    private static final String SERVICE_TYPE = "chat:ChatService";

    @Override
    public String kind() {
        return GOOGLE_CHAT;
    }

    @Override
    public Map<String, List<TextEdit>> addServiceInitSource(AddServiceInitModelContext context)
            throws WorkspaceDocumentException, FormatterException, IOException, BallerinaOpenApiException,
            EventSyncException {
        ServiceInitModel serviceInitModel = context.serviceInitModel();
        Map<String, Value> properties = serviceInitModel.getProperties();
        String listenerVarName = properties.get(KEY_LISTENER_VAR_NAME).getValue();

        // `listenOn` is the positional port; `auth` is a nested config record passed as the second
        // positional arg, matching the connector's own documented usage:
        // `new (8000, {auth: {path: "./service-account-key.json"}})`.
        List<String> args = new ArrayList<>();
        String listenOn = getPropertyValue(properties, "listenOn");
        if (!listenOn.isEmpty()) {
            args.add(listenOn);
        }
        String authFilePath = getPropertyValue(properties, "authFilePath");
        if (!authFilePath.isEmpty()) {
            args.add(String.format("{auth: {path: %s}}", authFilePath));
        }

        String listenerDeclaration = String.format("listener %s %s = new (%s);",
                LISTENER_TYPE, listenerVarName, String.join(", ", args));

        // The `@chat:ServiceConfig` annotation is required and must attach to the service, not the
        // listener.
        String endpointUrl = getPropertyValue(properties, "endpointUrl");
        String serviceConfigAnnotation = String.format("@chat:ServiceConfig {endpointUrl: %s}", endpointUrl);

        String serviceCode = NEW_LINE
                + listenerDeclaration
                + NEW_LINE
                + serviceConfigAnnotation
                + NEW_LINE
                + SERVICE + SPACE + SERVICE_TYPE + SPACE + ON + SPACE + listenerVarName + SPACE
                + OPEN_BRACE
                + NEW_LINE
                + CLOSE_BRACE + NEW_LINE;

        ModulePartNode modulePartNode = context.document().syntaxTree().rootNode();
        List<TextEdit> edits = new ArrayList<>();
        if (!importExists(modulePartNode, ORG, MODULE_NAME)) {
            edits.add(new TextEdit(Utils.toRange(modulePartNode.lineRange().startLine()),
                    getImportStmt(ORG, MODULE_NAME)));
        }
        edits.add(new TextEdit(Utils.toRange(modulePartNode.lineRange().endLine()), serviceCode));
        return Map.of(context.filePath(), edits);
    }

    private static String getPropertyValue(Map<String, Value> properties, String key) {
        Value property = properties.get(key);
        if (property != null && property.getValue() != null && !property.getValue().isEmpty()) {
            return property.getValue();
        }
        return "";
    }
}
