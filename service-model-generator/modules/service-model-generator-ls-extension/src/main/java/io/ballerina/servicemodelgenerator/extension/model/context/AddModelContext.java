package io.ballerina.servicemodelgenerator.extension.model.context;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.projects.Document;
import io.ballerina.projects.Project;
import io.ballerina.servicemodelgenerator.extension.model.Function;
import io.ballerina.servicemodelgenerator.extension.model.Service;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;

public record AddModelContext(Service service, Function function, SemanticModel semanticModel, Project project,
                              WorkspaceManager workspaceManager, String filePath, Document document,
                              NonTerminalNode node) {
}
