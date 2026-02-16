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

package io.ballerina.flowmodelgenerator.core.model.node;

import com.google.gson.Gson;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.flowmodelgenerator.core.model.FormBuilder;
import io.ballerina.flowmodelgenerator.core.model.NodeBuilder;
import io.ballerina.flowmodelgenerator.core.model.NodeKind;
import io.ballerina.flowmodelgenerator.core.model.Property;
import io.ballerina.flowmodelgenerator.core.model.SourceBuilder;
import io.ballerina.tools.text.LineRange;
import org.eclipse.lsp4j.TextEdit;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Represents the properties of a function definition node.
 *
 * @since 1.0.0
 */
public class TestFunctionDefinitionBuilder extends FunctionDefinitionBuilder {

    public static final String LABEL = "Test Function";
    public static final String DESCRIPTION = "Define a test function";

    public static final String FUNCTION_NAME_LABEL = "Name";
    public static final String FUNCTION_NAME_DOC = "Name of the function";

    public static final String PARAMETERS_LABEL = "Parameters";
    public static final String PARAMETERS_DOC = "Function parameters";

    @Override
    public void setConcreteConstData() {
        metadata().label(LABEL).description(DESCRIPTION);
        codedata().node(NodeKind.TEST_FUNCTION_DEFINITION);
    }

    @Override
    public void setConcreteTemplateData(TemplateContext context) {
        properties().functionNameTemplate("function", context.getAllVisibleSymbolNames(),
                FUNCTION_NAME_LABEL, FUNCTION_NAME_DOC);
        setMandatoryProperties(this, null, "", "");
        setOptionalProperties(this);
    }

    public static void setMandatoryProperties(NodeBuilder nodeBuilder, String returnType, String description,
                                              String returnDescription) {
        nodeBuilder.properties()
                .functionDescription(description)
                .returnType(returnType, null, true)
                .returnDescription(returnDescription)
                .nestedProperty();

        addAnnotationsSchema(nodeBuilder);
    }

    public static void setOptionalProperties(NodeBuilder nodeBuilder) {
        nodeBuilder.properties()
                .endNestedProperty(Property.ValueType.REPEATABLE_PROPERTY, Property.PARAMETERS_KEY, PARAMETERS_LABEL,
                        PARAMETERS_DOC, getParameterSchema(), true, false);
    }

    @Override
    public Map<Path, List<TextEdit>> toSource(SourceBuilder sourceBuilder) {
        return sourceBuilder.build();
    }

    private static void addAnnotationsSchema(NodeBuilder nodeBuilder) {
        nodeBuilder.properties().custom()
                .metadata()
                .label("Groups")
                .description("Groups to run")
                .stepOut()
                .type()
                .fieldType(Property.ValueType.EXPRESSION)
                .ballerinaType("string[]")
                .selected(true)
                .stepOut()
                .editable()
                .stepOut()
                .addProperty("groups");

        nodeBuilder.properties().custom()
                .metadata()
                .label("Enabled")
                .description("Enable/Disable the test")
                .stepOut()
                .type()
                .fieldType(Property.ValueType.FLAG)
                .ballerinaType("boolean")
                .selected(true)
                .stepOut()
                .editable()
                .stepOut()
                .addProperty("enabled");
    }
}
