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

package io.ballerina.flowmodelgenerator.core.model.node;

import io.ballerina.flowmodelgenerator.core.model.Codedata;
import io.ballerina.flowmodelgenerator.core.model.Metadata;
import io.ballerina.flowmodelgenerator.core.model.NodeBuilder;
import io.ballerina.flowmodelgenerator.core.model.NodeKind;
import io.ballerina.flowmodelgenerator.core.model.Property;
import io.ballerina.flowmodelgenerator.core.model.SourceBuilder;
import org.eclipse.lsp4j.TextEdit;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Sample node for smoke-testing the CONNECTION fieldType in the UI.
 *
 * <p>Renders three CONNECTION fields exercising each metadata shape:
 * <ul>
 *     <li>{@code anyConnection} — no {@code connectors} array: dropdown lists every connection,
 *         no Add button.</li>
 *     <li>{@code httpConnection} — one connector entry: dropdown filtered to HTTP, single Add button.</li>
 *     <li>{@code aiConnection} — two connector entries: union-filtered to Mistral or Milvus,
 *         two Add buttons.</li>
 * </ul>
 *
 * @since 1.8.0
 */
public class SampleConnectionNodeBuilder extends NodeBuilder {

    public static final String LABEL = "Sample Connection (Test)";
    public static final String DESCRIPTION = "Smoke-test node for the CONNECTION field type";
    private static final String NEW_LINE = System.lineSeparator();

    private static final String ANY_KEY = "anyConnection";
    private static final String HTTP_KEY = "httpConnection";
    private static final String AI_KEY = "aiConnection";

    @Override
    public void setConcreteConstData() {
        metadata().label(LABEL).description(DESCRIPTION);
        codedata().node(NodeKind.SAMPLE_CONNECTION);
    }

    @Override
    public void setConcreteTemplateData(TemplateContext context) {
        Codedata httpConnector = new Codedata.Builder<>(null)
                .node(NodeKind.NEW_CONNECTION)
                .org("ballerina").module("http").packageName("http")
                .object("Client").symbol("init").version("2.16.2")
                .build();
        Codedata mistralConnector = new Codedata.Builder<>(null)
                .node(NodeKind.NEW_CONNECTION)
                .org("ballerinax").module("mistral").packageName("mistral")
                .object("Client").symbol("init").version("1.0.2")
                .build();
        Codedata milvusConnector = new Codedata.Builder<>(null)
                .node(NodeKind.NEW_CONNECTION)
                .org("ballerinax").module("milvus").packageName("milvus")
                .object("Client").symbol("init").version("1.1.1")
                .build();

        // Field 1: no codedata — list all connections, no Add button.
        properties().connectionSelector(ANY_KEY, "Any Connection",
                "NEW_CONNECTION", "NEW_CONNECTION", null);

        // Field 2: one connector — filter to HTTP, single Add button.
        properties().connectionSelector(HTTP_KEY, "HTTP Connection",
                "NEW_CONNECTION", "NEW_CONNECTION",
                List.of(new Metadata.AllowedConnector(httpConnector, "Add new HTTP connection")));

        // Field 3: two connectors — union-filter to Mistral or Milvus, two Add buttons.
        properties().connectionSelector(AI_KEY, "AI Connection",
                "NEW_CONNECTION", "NEW_CONNECTION",
                List.of(
                        new Metadata.AllowedConnector(mistralConnector, "Add new Mistral connection"),
                        new Metadata.AllowedConnector(milvusConnector, "Add new Milvus connection")
                ));
    }

    @Override
    public Map<Path, List<TextEdit>> toSource(SourceBuilder sourceBuilder) {
        String anyValue = sourceBuilder.getProperty(ANY_KEY).map(Property::toSourceCode).orElse("()");
        String httpValue = sourceBuilder.getProperty(HTTP_KEY).map(Property::toSourceCode).orElse("()");
        String aiValue = sourceBuilder.getProperty(AI_KEY).map(Property::toSourceCode).orElse("()");
        String comment = "// Sample connection node - any: " + anyValue
                + ", http: " + httpValue
                + ", ai: " + aiValue
                + NEW_LINE;
        return sourceBuilder
                .token().name(NEW_LINE).name(comment).stepOut()
                .comment()
                .build();
    }
}
