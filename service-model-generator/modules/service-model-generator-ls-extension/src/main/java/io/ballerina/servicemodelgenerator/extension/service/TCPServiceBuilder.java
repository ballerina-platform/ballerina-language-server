package io.ballerina.servicemodelgenerator.extension.service;

import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.servicemodelgenerator.extension.model.AddModelContext;
import io.ballerina.servicemodelgenerator.extension.model.Service;
import io.ballerina.servicemodelgenerator.extension.model.Value;
import io.ballerina.servicemodelgenerator.extension.util.ListenerUtil;
import io.ballerina.servicemodelgenerator.extension.util.ServiceClassUtil;
import io.ballerina.servicemodelgenerator.extension.util.Utils;
import org.eclipse.lsp4j.TextEdit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static io.ballerina.servicemodelgenerator.extension.ServiceModelGeneratorConstants.NEW_LINE;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.BALLERINA;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.TCP;
import static io.ballerina.servicemodelgenerator.extension.util.ListenerUtil.getDefaultListenerDeclarationStmt;
import static io.ballerina.servicemodelgenerator.extension.util.ServiceModelUtils.populateRequiredFunctionsForServiceType;
import static io.ballerina.servicemodelgenerator.extension.util.Utils.FunctionAddContext.TCP_SERVICE_ADD;
import static io.ballerina.servicemodelgenerator.extension.util.Utils.getImportStmt;
import static io.ballerina.servicemodelgenerator.extension.util.Utils.getServiceDeclarationNode;
import static io.ballerina.servicemodelgenerator.extension.util.Utils.importExists;
import static io.ballerina.servicemodelgenerator.extension.util.Utils.populateRequiredFuncsDesignApproachAndServiceType;

public final class TCPServiceBuilder extends AbstractServiceBuilder {

    private static final String TCP_SERVICE_CLASS_NAME = "TcpEchoService";

    @Override
    public Map<String, List<TextEdit>> addModel(AddModelContext context) throws Exception {
        List<TextEdit> edits = new ArrayList<>();
        ListenerUtil.DefaultListener defaultListener = ListenerUtil.getDefaultListener(context);
        if (Objects.nonNull(defaultListener)) {
            String stmt = getDefaultListenerDeclarationStmt(defaultListener);
            edits.add(new TextEdit(Utils.toRange(defaultListener.linePosition()), stmt));
        }

        Service service = context.service();
        populateRequiredFuncsDesignApproachAndServiceType(service);
        populateRequiredFunctionsForServiceType(service);

        ModulePartNode rootNode = context.document().syntaxTree().rootNode();
        String serviceName = Utils.generateTypeIdentifier(context.semanticModel(), context.document(),
                rootNode.lineRange().endLine(), TCP_SERVICE_CLASS_NAME);
        service.getProperties().put("returningServiceClass", Value.getTcpValue(serviceName)); // TODO: remove this prop

        Map<String, String> importsNeedForTypes = new HashMap<>();
        String serviceDeclaration = getServiceDeclarationNode(service, TCP_SERVICE_ADD, importsNeedForTypes);
        edits.add(new TextEdit(Utils.toRange(rootNode.lineRange().endLine()), NEW_LINE + serviceDeclaration));

        Set<String> importStmts = new HashSet<>();
        if (!importExists(rootNode, BALLERINA, TCP)) {
            importStmts.add(Utils.getImportStmt(service.getOrgName(), service.getModuleName()));
        }
        importsNeedForTypes.values().forEach(moduleId -> {
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

        String serviceClass = ServiceClassUtil.getTcpConnectionServiceTemplate().formatted(serviceName);
        edits.add(new TextEdit(Utils.toRange(rootNode.lineRange().endLine()), serviceClass));

        return Map.of(context.filePath(), edits);
    }

    @Override
    public String kind() {
        return "tcp";
    }
}
