package io.ballerina.servicemodelgenerator.extension.diagnostics;

import com.google.gson.JsonElement;
import io.ballerina.servicemodelgenerator.extension.request.ServiceDesignerDiagnosticRequest;
import io.ballerina.servicemodelgenerator.extension.response.ServiceDesignerDiagnosticResponse;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;

import java.util.concurrent.Callable;

public class DebouncedDiagnosticRequest implements Callable<ServiceDesignerDiagnosticResponse> {

    private final WorkspaceManager workspaceManager;
    private final ServiceDesignerDiagnosticRequest request;

    public DebouncedDiagnosticRequest(WorkspaceManager workspaceManager,
                                       ServiceDesignerDiagnosticRequest request) {
        this.workspaceManager = workspaceManager;
        this.request = request;
    }

    @Override
    public ServiceDesignerDiagnosticResponse call() throws Exception {
        return new ServiceDesignerDiagnosticResponse(DiagnosticsHandler.getDiagnostics(request, workspaceManager));
    }

    public String getKey() {
        return request.operation();
    }

    public long getDelay() {
        return 1000; // 1 second delay
    }
}
