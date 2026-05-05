/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com)
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.scanner.extension;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.scanner.extension.codeaction.ScannerCodeAction;
import io.ballerina.scanner.extension.codeaction.ScannerCodeActionProvider;
import io.ballerina.scanner.extension.request.AddExclusionRequest;
import io.ballerina.scanner.extension.request.AddGlobalExclusionRequest;
import io.ballerina.scanner.extension.request.CodeActionRequest;
import io.ballerina.scanner.extension.request.RemoveExclusionRequest;
import io.ballerina.scanner.extension.request.RemoveGlobalExclusionRequest;
import io.ballerina.scanner.extension.request.ScanRequest;
import io.ballerina.scanner.extension.response.AddExclusionResponse;
import io.ballerina.scanner.extension.response.AddGlobalExclusionResponse;
import io.ballerina.scanner.extension.response.CodeActionResponse;
import io.ballerina.scanner.extension.response.RemoveExclusionResponse;
import io.ballerina.scanner.extension.response.RemoveGlobalExclusionResponse;
import io.ballerina.scanner.extension.response.ScanResponse;
import org.ballerinalang.annotation.JavaSPIService;
import org.ballerinalang.langserver.BallerinaLanguageServer;
import org.ballerinalang.langserver.LSClientLogger;
import org.ballerinalang.langserver.commons.LanguageServerContext;
import org.ballerinalang.langserver.commons.client.ExtendedLanguageClient;
import org.ballerinalang.langserver.commons.service.spi.ExtendedLanguageServerService;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.jsonrpc.services.JsonSegment;
import org.eclipse.lsp4j.services.LanguageServer;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@JavaSPIService("org.ballerinalang.langserver.commons.service.spi.ExtendedLanguageServerService")
@JsonSegment("scanner")
public class ScannerService implements ExtendedLanguageServerService {

    private static final Gson GSON = new Gson();

    private WorkspaceManager workspaceManager;
    private LSClientLogger clientLogger;
    private Supplier<ExtendedLanguageClient> languageClientSupplier;

    // Scanner reflection
    private URLClassLoader scannerLoader;
    private Method scanMethod;
    private Method addExclusionMethod;
    private Method addGlobalExclusionMethod;
    private Method removeExclusionMethod;
    private Method removeGlobalExclusionMethod;
    private boolean scannerAvailable;

    // Code action support
    private ScannerCodeActionProvider codeActionProvider;

    @Override
    public void init(LanguageServer langServer,
                     WorkspaceManager workspaceManager,
                     LanguageServerContext serverContext) {
        this.workspaceManager = workspaceManager;
        this.clientLogger = LSClientLogger.getInstance(serverContext);
        ScannerUtils.setClientLogger(this.clientLogger);
        this.languageClientSupplier = () -> {
            ExtendedLanguageClient languageClient = serverContext.get(ExtendedLanguageClient.class);
            if (languageClient != null) {
                return languageClient;
            }
            if (langServer instanceof BallerinaLanguageServer ballerinaLanguageServer) {
                return ballerinaLanguageServer.getClient();
            }
            return null;
        };

        // Load scanner JAR and resolve methods once
        loadScanner();
    }

    /**
     * Loads the scanner JAR and resolves the ScanTool methods once.
     * * @return true if successfully loaded, false otherwise
     */
    private boolean loadScanner() {
        try {
            File scannerJar = ScannerUtils.resolveScannerJar();
            if (scannerJar == null || !scannerJar.exists()) {
                ScannerUtils.logError("Scanner JAR not found. Scanner aborted.");
                this.scannerAvailable = false;
                return false;
            }

            URL[] urls = {scannerJar.toURI().toURL()};
            this.scannerLoader = new URLClassLoader(urls, this.getClass().getClassLoader());

            Class<?> scanToolClass = scannerLoader.loadClass("io.ballerina.scan.internal.ScanTool");

            this.scanMethod = scanToolClass.getMethod("runScan", String.class, Map.class);
            this.addExclusionMethod = scanToolClass.getMethod("addExclusion", String.class,
                    String.class, int.class, String.class, Map.class);
            this.addGlobalExclusionMethod = scanToolClass.getMethod("addGlobalExclusion",
                    String.class, String.class);
            this.removeExclusionMethod = scanToolClass.getMethod("removeExclusion", String.class,
                    String.class, String.class, String.class, String.class);
            this.removeGlobalExclusionMethod = scanToolClass.getMethod("removeGlobalExclusion",
                    String.class, String.class);

            this.scannerAvailable = true;
            ScannerUtils.logInfo("Scanner JAR loaded successfully from: " + scannerJar.getAbsolutePath());

            // Initialize code action provider
            this.codeActionProvider = new ScannerCodeActionProvider();
            ScannerUtils.logInfo("Scanner code action provider loaded successfully");
            return true;

        } catch (ReflectiveOperationException | IOException e) {
            ScannerUtils.logError("Failed to initialize scanner: " + e.getMessage());
            this.scannerAvailable = false;
            return false;
        }
    }

    @Override
    public Class<?> getRemoteInterface() {
        return getClass();
    }

    @JsonRequest
    public CompletableFuture<ScanResponse> getVulnerabilities(ScanRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            ScanResponse response = new ScanResponse();

            ScannerUtils.logInfo("Received request for: " + request.getDocumentUri());

            if (!scannerAvailable) {
                if (!loadScanner()) {
                    response.setError("Scanner tool not found. Pull it from Ballerina Central.");
                    return response;
                }
            }

            try {
                // Resolve file path and project root
                Path filePath = getPathFromURI(request.getDocumentUri());
                Path projectRoot = findProjectRoot(filePath);

                if (projectRoot == null) {
                    throw new RuntimeException("Unable to determine project root for: " + filePath);
                }

                // Prepare Build Options Map
                Map<String, Boolean> buildOptionsMap = new java.util.HashMap<>();
                buildOptionsMap.put("offline", request.isOffline());
                buildOptionsMap.put("sticky", request.isSticky());
                buildOptionsMap.put("skipTests", request.isSkipTests());
                buildOptionsMap.put("applyUnsavedChanges", false);

                // Invoke scanner with context classloader for compiler plugins
                ScannerUtils.logInfo("Invoking scanner...");

                ClassLoader originalCL = Thread.currentThread().getContextClassLoader();
                Object result;
                try {
                    Thread.currentThread().setContextClassLoader(scannerLoader);
                    result = scanMethod.invoke(null, projectRoot.toString(), buildOptionsMap);
                } finally {
                    Thread.currentThread().setContextClassLoader(originalCL);
                }

                // Handle result
                String jsonResult;
                if (result instanceof String) {
                    jsonResult = (String) result;
                } else {
                    ScannerUtils.logError("Unknown Error: Could not parse scanner tool result");
                    response.setError("Unknown Error: Could not parse scanner tool result");
                    return response;
                }

                // Parse JSON into ScannerIssueContext list
                Type listType = new TypeToken<List<ScannerIssueContext>>() { }.getType();
                Type exclusionListType = new TypeToken<List<ScannerExclusionContext>>() { }.getType();

                List<ScannerIssueContext> activeIssues = new java.util.ArrayList<>();
                List<ScannerExclusionContext> excludedIssues = new java.util.ArrayList<>();

                try {
                    JsonElement element = com.google.gson.JsonParser.parseString(jsonResult);
                    if (element.isJsonObject()) {
                        JsonObject resultObj = element.getAsJsonObject();

                        // Check for scanner-side execution errors
                        if (resultObj.has("success") && !resultObj.get("success").getAsBoolean()) {
                            String errorMsg = resultObj.has("error") ? resultObj.get("error")
                                                                            .getAsString() : "Unknown scanner error";
                            ScannerUtils.logError("Scanner returned an error: " + errorMsg);
                            response.setError(errorMsg);
                            return response;
                        }

                        if (resultObj.has("activeIssues")) {
                            activeIssues = GSON.fromJson(resultObj.get("activeIssues"), listType);
                        }
                        if (resultObj.has("excludedIssues")) {
                            excludedIssues = GSON.fromJson(resultObj.get("excludedIssues"), exclusionListType);
                        }
                    } else if (element.isJsonArray()) {
                        activeIssues = GSON.fromJson(jsonResult, listType);
                    }
                } catch (Exception e) {
                    ScannerUtils.logError("Failed to parse scan result: " + e.getMessage());
                    response.setError("Failed to parse scan result: " + e.getMessage());
                }

                if (activeIssues == null) {
                    activeIssues = List.of();
                }
                if (excludedIssues == null) {
                    excludedIssues = List.of();
                }

                // Publish Diagnostics
                if (request.isPublishDiagnostics()) {
                    ExtendedLanguageClient languageClient =
                            languageClientSupplier != null ? languageClientSupplier.get() : null;
                    if (languageClient != null) {
                        ScannerUtils.publishDiagnostics(activeIssues, languageClient);
                        ScannerUtils.logInfo("Issues published.");
                    }
                }

                response.setActiveIssues(activeIssues);
                response.setExcludedIssues(excludedIssues);
                ScannerUtils.logInfo("Scan complete. Active Issues: " + activeIssues.size()
                        + ", Excluded Issues: " + excludedIssues.size());

            } catch (java.lang.reflect.InvocationTargetException e) {
                Throwable realCause = e.getCause() != null ? e.getCause() : e;
                ScannerUtils.logError("Scanner Internal Error: "
                                                    + realCause.getClass().getName() + ": " + realCause.getMessage());
                response.setError(realCause);
            } catch (Throwable e) {
                ScannerUtils.logError("Scanner Execution Error: " + e.getMessage());
                response.setError(e);
            }
            return response;
        });
    }

    @JsonRequest
    public CompletableFuture<CodeActionResponse> getCodeActions(CodeActionRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            CodeActionResponse response = new CodeActionResponse();

            ScannerUtils.logInfo("Code action request for rule: "
                    + request.getRuleId()
                    + " in " + request.getDocumentUri());

            if (!scannerAvailable) {
                if (!loadScanner()) {
                    response.setError("Scanner tool not found. Pull it from Ballerina Central.");
                    ScannerUtils.notifyError("Scanner tool not found. Pull it from Ballerina Central.");
                    return response;
                }
            }

            try {
                // Resolve the file path
                Path filePath = getPathFromURI(request.getDocumentUri());

                // Get the syntax tree from the workspace manager
                java.util.Optional<SyntaxTree> syntaxTreeOpt = workspaceManager.syntaxTree(filePath);

                if (syntaxTreeOpt.isEmpty()) {
                    String message = "Unable to get syntax tree for: " + request.getDocumentUri();
                    ScannerUtils.logError(message);
                    response.setError(message);
                    return response;
                }

                // Delegate to the code action provider
                List<ScannerCodeAction> actions = codeActionProvider.getCodeActions(
                        request.getRuleId(),
                        syntaxTreeOpt.get(),
                        request.getStartLine(),
                        request.getStartColumn(),
                        request.getEndLine(),
                        request.getEndColumn());

                response.setCodeActions(actions);
                ScannerUtils.logInfo("Returning " 
                                      + actions.size() + " code action(s) for rule: "
                                      + request.getRuleId());

            } catch (Exception e) {
                ScannerUtils.logError("Code action error: " + e.getMessage());
                response.setError(e);
            }
            return response;
        });
    }

    @JsonRequest
    public CompletableFuture<AddExclusionResponse> addExclusion(AddExclusionRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            AddExclusionResponse response = new AddExclusionResponse();

            ScannerUtils.logInfo("Received addExclusion request for rule: "
                + request.getRuleId() + " in " + request.getDocumentUri());

            if (!scannerAvailable) {
                if (!loadScanner()) {
                    response.setError("Scanner tool not found. Pull it from Ballerina Central.");
                    ScannerUtils.notifyError("Scanner tool not found. Pull it from Ballerina Central.");
                    return response;
                }
            }

            if (addExclusionMethod == null) {
                response.setError("Method unavailable: AddExclusion");
                return response;
            }

            try {
                // Resolve file path and project root
                Path filePath = getPathFromURI(request.getDocumentUri());
                Path projectRoot = findProjectRoot(filePath);

                if (projectRoot == null) {
                    throw new RuntimeException("Unable to determine project root for: " + filePath);
                }

                String relativeFilePath = projectRoot.relativize(filePath).toString();

                Map<String, Boolean> buildOptionsMap = new java.util.HashMap<>();
                buildOptionsMap.put("offline", request.isOffline());
                buildOptionsMap.put("sticky", request.isSticky());
                buildOptionsMap.put("skipTests", request.isSkipTests());
                buildOptionsMap.put("applyUnsavedChanges", false);

                // Invoke with context classloader
                ClassLoader originalCL = Thread.currentThread().getContextClassLoader();
                Object result;
                try {
                    Thread.currentThread().setContextClassLoader(scannerLoader);
                    result = addExclusionMethod.invoke(null, projectRoot.toString(), relativeFilePath,
                                                       request.getLineNumber(), request.getRuleId(),
                                                       buildOptionsMap);
                } finally {
                    Thread.currentThread().setContextClassLoader(originalCL);
                }

                if (result instanceof String jsonResult) {
                    JsonObject resultObj = com.google.gson.JsonParser.parseString(jsonResult).getAsJsonObject();
                    if (resultObj.has("success") && !resultObj.get("success").getAsBoolean()) {
                        String errorMsg = resultObj.has("error") ? resultObj.get("error")
                                                                            .getAsString() : "Unknown scanner error";
                        ScannerUtils.logError("Scanner returned an error: " + errorMsg);
                        response.setError(errorMsg);
                        return response;
                    }
                    response = GSON.fromJson(jsonResult, AddExclusionResponse.class);
                    ScannerUtils.logInfo("Exclusion added for rule: " + request.getRuleId());
                } else {
                    response.setError("Invalid response from scanner tool.");
                }

            } catch (java.lang.reflect.InvocationTargetException e) {
                Throwable realCause = e.getCause() != null ? e.getCause() : e;
                ScannerUtils.logError("Scanner Internal Error: "
                                                      + realCause.getClass().getName() + ": " + realCause.getMessage());
                response.setError("Scanner Internal Error: " + realCause.getMessage());
            } catch (Throwable e) {
                ScannerUtils.logError("Scanner Execution Error: " + e.getMessage());
                response.setError("Scanner Execution Error: " + e.getMessage());
            }
            return response;
        });
    }

    @JsonRequest
    public CompletableFuture<AddGlobalExclusionResponse> addGlobalExclusion(AddGlobalExclusionRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            AddGlobalExclusionResponse response = new AddGlobalExclusionResponse();

            ScannerUtils.logInfo("Received addGlobalExclusion request for rule: "
                + request.getRuleId() + " in " + request.getDocumentUri());

            if (!scannerAvailable) {
                if (!loadScanner()) {
                    response.setError("Scanner tool not found. Pull it from Ballerina Central.");
                    ScannerUtils.notifyError("Scanner tool not found. Pull it from Ballerina Central.");
                    return response;
                }
            }

            if (addGlobalExclusionMethod == null) {
                response.setError("Method unavailable: addGlobalExclusion");
                return response;
            }

            try {
                // Resolve file path and project root
                Path filePath = getPathFromURI(request.getDocumentUri());
                Path projectRoot = findProjectRoot(filePath);

                if (projectRoot == null) {
                    throw new RuntimeException("Unable to determine project root for: " + filePath);
                }

                // Invoke with context classloader
                ClassLoader originalCL = Thread.currentThread().getContextClassLoader();
                Object result;
                try {
                    Thread.currentThread().setContextClassLoader(scannerLoader);
                    result = addGlobalExclusionMethod.invoke(null, projectRoot.toString(), request.getRuleId());
                } finally {
                    Thread.currentThread().setContextClassLoader(originalCL);
                }

                if (result instanceof String jsonResult) {
                    JsonObject resultObj = com.google.gson.JsonParser.parseString(jsonResult).getAsJsonObject();
                    if (resultObj.has("success") && !resultObj.get("success").getAsBoolean()) {
                        String errorMsg = resultObj.has("error") ? resultObj.get("error")
                                                                            .getAsString() : "Unknown scanner error";
                        ScannerUtils.logError("Scanner returned an error: " + errorMsg);
                        response.setError(errorMsg);
                        return response;
                    }
                    response = GSON.fromJson(jsonResult, AddGlobalExclusionResponse.class);
                    ScannerUtils.logInfo("Global exclusion added for rule: " + request.getRuleId());
                } else {
                    response.setError("Invalid response from scanner tool.");
                }

            } catch (java.lang.reflect.InvocationTargetException e) {
                Throwable realCause = e.getCause() != null ? e.getCause() : e;
                ScannerUtils.logError("Scanner Internal Error: "
                                                    + realCause.getClass().getName() + ": " + realCause.getMessage());
                response.setError("Scanner Internal Error: " + realCause.getMessage());
            } catch (Throwable e) {
                ScannerUtils.logError("Scanner Execution Error: " + e.getMessage());
                response.setError("Scanner Execution Error: " + e.getMessage());
            }
            return response;
        });
    }

    @JsonRequest
    public CompletableFuture<RemoveExclusionResponse> removeExclusion(RemoveExclusionRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            RemoveExclusionResponse response = new RemoveExclusionResponse();

            ScannerUtils.logInfo("Received removeExclusion request for rule: "
                + request.getRuleId() + " in " + request.getDocumentUri());

            if (!scannerAvailable) {
                if (!loadScanner()) {
                    response.setError("Scanner tool not found. Pull it from Ballerina Central.");
                    ScannerUtils.notifyError("Scanner tool not found. Pull it from Ballerina Central.");
                    return response;
                }
            }

            if (removeExclusionMethod == null) {
                response.setError("Method unavailable: removeExclusion");
                return response;
            }

            try {
                Path filePath = getPathFromURI(request.getDocumentUri());
                Path projectRoot = findProjectRoot(filePath);

                if (projectRoot == null) {
                    throw new RuntimeException("Unable to determine project root for: " + filePath);
                }

                String relativeFilePath = projectRoot.relativize(filePath).toString();

                ClassLoader originalCL = Thread.currentThread().getContextClassLoader();
                Object result;
                try {
                    Thread.currentThread().setContextClassLoader(scannerLoader);
                    result = removeExclusionMethod.invoke(null, projectRoot.toString(), relativeFilePath,
                                                          request.getRuleId(), request.getSymbol(),
                                                          request.getLineHash());
                } finally {
                    Thread.currentThread().setContextClassLoader(originalCL);
                }

                if (result instanceof String jsonResult) {
                    JsonObject resultObj = com.google.gson.JsonParser.parseString(jsonResult).getAsJsonObject();
                    if (resultObj.has("success") && !resultObj.get("success").getAsBoolean()) {
                        String errorMsg = resultObj.has("error") ? resultObj.get("error")
                                                                            .getAsString() : "Unknown scanner error";
                        ScannerUtils.logError("Scanner returned an error: " + errorMsg);
                        response.setError(errorMsg);
                        return response;
                    }
                    response = GSON.fromJson(jsonResult, RemoveExclusionResponse.class);
                    ScannerUtils.logInfo("Exclusion removed for rule: " + request.getRuleId());
                } else {
                    response.setError("Invalid response from scanner tool.");
                }

            } catch (java.lang.reflect.InvocationTargetException e) {
                Throwable realCause = e.getCause() != null ? e.getCause() : e;
                ScannerUtils.logError("Scanner Internal Error: "
                                                    + realCause.getClass().getName() + ": " + realCause.getMessage());
                response.setError("Scanner Internal Error: " + realCause.getMessage());
            } catch (Throwable e) {
                ScannerUtils.logError("Scanner Execution Error: " + e.getMessage());
                response.setError("Scanner Execution Error: " + e.getMessage());
            }
            return response;
        });
    }

    @JsonRequest
    public CompletableFuture<RemoveGlobalExclusionResponse> removeGlobalExclusion(
            RemoveGlobalExclusionRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            RemoveGlobalExclusionResponse response = new RemoveGlobalExclusionResponse();

            ScannerUtils.logInfo("Received removeGlobalExclusion request for rule: "
                + request.getRuleId() + " in " + request.getDocumentUri());

            if (!scannerAvailable) {
                if (!loadScanner()) {
                    response.setError("Scanner tool not found. Pull it from Ballerina Central.");
                    ScannerUtils.notifyError("Scanner tool not found. Pull it from Ballerina Central.");
                    return response;
                }
            }

            if (removeGlobalExclusionMethod == null) {
                response.setError("Method unavailable: removeGlobalExclusion");
                return response;
            }

            try {
                Path filePath = getPathFromURI(request.getDocumentUri());
                Path projectRoot = findProjectRoot(filePath);

                if (projectRoot == null) {
                    throw new RuntimeException("Unable to determine project root for: " + filePath);
                }

                ClassLoader originalCL = Thread.currentThread().getContextClassLoader();
                Object result;
                try {
                    Thread.currentThread().setContextClassLoader(scannerLoader);
                    result = removeGlobalExclusionMethod.invoke(null, projectRoot.toString(), request.getRuleId());
                } finally {
                    Thread.currentThread().setContextClassLoader(originalCL);
                }

                if (result instanceof String jsonResult) {
                    JsonObject resultObj = com.google.gson.JsonParser.parseString(jsonResult).getAsJsonObject();
                    if (resultObj.has("success") && !resultObj.get("success").getAsBoolean()) {
                        String errorMsg = resultObj.has("error") ? resultObj.get("error")
                                                                            .getAsString() : "Unknown scanner error";
                        ScannerUtils.logError("Scanner returned an error: " + errorMsg);
                        response.setError(errorMsg);
                        return response;
                    }
                    response = GSON.fromJson(jsonResult, RemoveGlobalExclusionResponse.class);
                    ScannerUtils.logInfo("Global exclusion removed for rule: " + request.getRuleId());
                } else {
                    response.setError("Invalid response from scanner tool.");
                }

            } catch (java.lang.reflect.InvocationTargetException e) {
                Throwable realCause = e.getCause() != null ? e.getCause() : e;
                ScannerUtils.logError("Scanner Internal Error: "
                                                    + realCause.getClass().getName() + ": " + realCause.getMessage());
                response.setError("Scanner Internal Error: " + realCause.getMessage());
            } catch (Throwable e) {
                ScannerUtils.logError("Scanner Execution Error: " + e.getMessage());
                response.setError("Scanner Execution Error: " + e.getMessage());
            }
            return response;
        });
    }

    // ===================================================================================
    // HELPER METHODS
    // ===================================================================================

    /**
     * Walks up the directory tree from the given file path looking for a
     * {@code Ballerina.toml} file. Returns the directory that contains it,
     * which is the Ballerina package root. Returns {@code null} if none is found.
     */
    private static Path findProjectRoot(Path filePath) {
        Path dir = filePath.toAbsolutePath().normalize();
        // Start from the file's parent directory
        if (!java.nio.file.Files.isDirectory(dir)) {
            dir = dir.getParent();
        }
        while (dir != null) {
            if (java.nio.file.Files.exists(dir.resolve("Ballerina.toml"))) {
                return dir;
            }
            dir = dir.getParent();
        }
        return null;
    }

    private Path getPathFromURI(String uri) {
        String normalizedUri = uri == null ? "" : uri.trim();

        try {
            // Encode unencoded spaces often sent by LSP clients before parsing.
            URI parsedUri = URI.create(normalizedUri.replace(" ", "%20"));
            if ("file".equalsIgnoreCase(parsedUri.getScheme())) {
                return Paths.get(parsedUri);
            }
        } catch (Exception ignored) {
            // Fall back to manual normalization for non-standard client inputs.
        }

        String decodedPath = java.net.URLDecoder.decode(
                normalizedUri,
                java.nio.charset.StandardCharsets.UTF_8
        );

        if (decodedPath.startsWith("file://")) {
            decodedPath = decodedPath.substring("file://".length());
        }

        // Windows file URIs can become /C:/... after stripping file://.
        if (decodedPath.startsWith("/") && decodedPath.length() > 2 && decodedPath.charAt(2) == ':') {
            decodedPath = decodedPath.substring(1);
        }

        return Paths.get(decodedPath);
    }
}
