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

package io.ballerina.servicemodelgenerator.extension.diagnostics;

import io.ballerina.servicemodelgenerator.extension.request.ServiceDesignerDiagnosticRequest;
import io.ballerina.servicemodelgenerator.extension.response.ServiceDesignerDiagnosticResponse;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;

import java.util.concurrent.Callable;

/**
 * Represents a debounced diagnostic request for the service designer.
 * This class implements Callable to allow execution in a separate thread with a delay.
 *
 * @since 1.2.0
 */
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
