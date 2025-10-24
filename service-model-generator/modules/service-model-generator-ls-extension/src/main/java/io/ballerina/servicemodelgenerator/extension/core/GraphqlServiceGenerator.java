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

package io.ballerina.servicemodelgenerator.extension.core;

import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import graphql.schema.idl.errors.SchemaProblem;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.MethodSymbol;
import io.ballerina.compiler.api.symbols.ObjectTypeSymbol;
import io.ballerina.compiler.api.symbols.ResourceMethodSymbol;
import io.ballerina.compiler.api.symbols.TypeDefinitionSymbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.graphql.generator.service.diagnostic.ServiceDiagnosticMessages;
import io.ballerina.graphql.generator.service.exception.ServiceGenerationException;
import io.ballerina.graphql.generator.service.generator.ServiceCodeGenerator;
import io.ballerina.graphql.generator.utils.SrcFilePojo;
import io.ballerina.modelgenerator.commons.PackageUtil;
import io.ballerina.projects.Document;
import io.ballerina.projects.DocumentConfig;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Module;
import io.ballerina.projects.ModuleId;
import io.ballerina.projects.ModuleName;
import io.ballerina.projects.Package;
import io.ballerina.projects.Project;
import io.ballerina.servicemodelgenerator.extension.model.ServiceInitModel;
import io.ballerina.servicemodelgenerator.extension.util.Utils;
import org.ballerinalang.langserver.commons.eventsync.exceptions.EventSyncException;
import org.ballerinalang.langserver.commons.workspace.WorkspaceDocumentException;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;
import org.eclipse.lsp4j.TextEdit;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static io.ballerina.servicemodelgenerator.extension.core.OpenApiServiceGenerator.BALLERINA_LANG;
import static io.ballerina.servicemodelgenerator.extension.core.OpenApiServiceGenerator.CLOSE_BRACE;
import static io.ballerina.servicemodelgenerator.extension.core.OpenApiServiceGenerator.COLON;
import static io.ballerina.servicemodelgenerator.extension.core.OpenApiServiceGenerator.LS;
import static io.ballerina.servicemodelgenerator.extension.core.OpenApiServiceGenerator.SERVICE_DECLARATION;
import static io.ballerina.servicemodelgenerator.extension.core.OpenApiServiceGenerator.getParentModuleName;
import static io.ballerina.servicemodelgenerator.extension.core.OpenApiServiceGenerator.getServiceTypeSymbol;
import static io.ballerina.servicemodelgenerator.extension.core.OpenApiServiceGenerator.sanitizePackageNames;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.NEW_LINE;
import static io.ballerina.servicemodelgenerator.extension.util.Utils.importExists;

public class GraphqlServiceGenerator {

    private final Path projectPath;
    private final WorkspaceManager workspaceManager;

    public static final String MAIN_BAL = "main.bal";

    public GraphqlServiceGenerator(Path projectPath, WorkspaceManager workspaceManager) {
        this.projectPath = projectPath;
        this.workspaceManager = workspaceManager;
    }

    public Map<String, List<TextEdit>> generateService(ServiceInitModel serviceInitModel, String path, String listeners,
                                                       String listenerDeclaration) throws IOException,
            SchemaProblem, ServiceGenerationException, WorkspaceDocumentException, EventSyncException {

        SchemaParser schemaParser = new SchemaParser();
        SchemaGenerator schemaGenerator = new SchemaGenerator();
        TypeDefinitionRegistry typeRegistry;
        String schema = serviceInitModel.getGraphqlSchema().getValue();

        String sdlInput = extractSchemaContent(schema);
        typeRegistry = schemaParser.parse(sdlInput);
        GraphQLSchema graphqlSchema = schemaGenerator.makeExecutableSchema(typeRegistry, RuntimeWiring.MOCKED_WIRING);

        List<SrcFilePojo> srcFiles = new ArrayList<>();
        ServiceCodeGenerator svcCodeGenerator = new ServiceCodeGenerator();
        svcCodeGenerator.generateServiceTypes("", "types.bal", graphqlSchema, srcFiles);
        return generateService(srcFiles.getFirst(), path, listeners, listenerDeclaration);
    }

    private Map<String, List<TextEdit>> generateService(SrcFilePojo srcFile, String path, String listeners,
                                                        String listenerDeclaration)
            throws WorkspaceDocumentException, EventSyncException, ServiceGenerationException {

        Path mainFile = projectPath.resolve(MAIN_BAL);
        Map<String, List<TextEdit>> textEditsMap = new LinkedHashMap<>();
        Project project = this.workspaceManager.loadProject(mainFile);
        Optional<Document> document = this.workspaceManager.document(mainFile);
        Optional<SemanticModel> semanticModel = this.workspaceManager.semanticModel(mainFile);
        if (document.isPresent() && semanticModel.isPresent()) {
            List<TextEdit> textEdits = new ArrayList<>();
            ModulePartNode modulePartNode = document.get().syntaxTree().rootNode();

            if (!importExists(modulePartNode, "ballerina", "graphql")) {
                String importText = Utils.getImportStmt("ballerina", "graphql");
                textEdits.add(new TextEdit(Utils.toRange(modulePartNode.lineRange().startLine()), importText));
            }
            String serviceImplContent = genServiceImplementation(srcFile, path, listeners, project,
             mainFile);
            StringBuilder builder = new StringBuilder(NEW_LINE);
            if (Objects.nonNull(listenerDeclaration)) {
                builder.append(listenerDeclaration).append(NEW_LINE);
            }
            builder.append(serviceImplContent);
            textEdits.add(new TextEdit(Utils.toRange(modulePartNode.lineRange().endLine()), builder.toString()));
            textEditsMap.put(mainFile.toAbsolutePath().toString(), textEdits);
        }
        return textEditsMap;
    }

    private String genServiceImplementation(SrcFilePojo serviceType, String path, String listeners,
                                            Project project, Path mainFile) throws ServiceGenerationException {
        Package currentPackage = project.currentPackage();
        Module module = currentPackage.module(ModuleName.from(currentPackage.packageName()));
        ModuleId moduleId = module.moduleId();
        DocumentId serviceObjDocId = DocumentId.create(mainFile.toString(), moduleId);
        DocumentConfig documentConfig = DocumentConfig.from(
                serviceObjDocId, serviceType.getContent(), serviceType.getFileName());
        module.modify().addDocument(documentConfig).apply();

        SemanticModel semanticModel = PackageUtil.getCompilation(project).getSemanticModel(moduleId);
        TypeDefinitionSymbol symbol = getServiceTypeSymbol(semanticModel.moduleSymbols(), "schema");
        if (symbol == null) {
            throw new ServiceGenerationException(ServiceDiagnosticMessages.GRAPHQL_SERVICE_GEN_100, null,
                    "Cannot find service type definition");
        }

        TypeSymbol typeSymbol = symbol.typeDescriptor();
        if (typeSymbol.typeKind() != TypeDescKind.OBJECT) {
            throw new ServiceGenerationException(ServiceDiagnosticMessages.GRAPHQL_SERVICE_GEN_100, null,
                    "Cannot find service object type definition");
        }

        Map<String, MethodSymbol> methodSymbolMap = ((ObjectTypeSymbol) typeSymbol).methods();
        StringBuilder serviceImpl = new StringBuilder();
        serviceImpl.append(String.format(SERVICE_DECLARATION, path, listeners));
        serviceImpl.append(LS);
        for (Map.Entry<String, MethodSymbol> entry : methodSymbolMap.entrySet()) {
            MethodSymbol methodSymbol = entry.getValue();
            if (methodSymbol instanceof ResourceMethodSymbol resourceMethodSymbol) {
                serviceImpl.append(getResourceFunction(resourceMethodSymbol, getParentModuleName(symbol)));
            }
        }
        serviceImpl.append(CLOSE_BRACE).append(LS);
        return serviceImpl.toString();
    }

    private String getResourceFunction(ResourceMethodSymbol resourceMethodSymbol, String parentModuleName) {
        String resourceSignature = resourceMethodSymbol.signature();
        if (Objects.nonNull(parentModuleName)) {
            resourceSignature = resourceSignature.replace(parentModuleName + COLON, "");
        }
        if (resourceSignature.contains(BALLERINA_LANG)) {
            resourceSignature = resourceSignature.replace(BALLERINA_LANG + ".", "");
            resourceSignature = resourceSignature.replaceAll("\\d+\\.\\d+\\.\\d+:", "");
        }
        return genResourceFunctionBody(resourceSignature);
    }

    private String genResourceFunctionBody(String resourceSignature) {
        return LS + "\t" + sanitizePackageNames(resourceSignature) + " {" + LS + "\t}" + LS;
    }


    /**
     * Extracts the schema content.
     *
     * @param schema                                the schema value of the Graphql config file
     * @return                                      the schema content
     * @throws java.io.IOException                          If an I/O error occurs
     *
     * since 1.4.0
     */
    public static String extractSchemaContent(String schema) throws IOException {
        File schemaFile = new File(schema);
        Path schemaPath = Paths.get(schemaFile.getCanonicalPath());
        return String.join(NEW_LINE, Files.readAllLines(schemaPath));
    }
}
