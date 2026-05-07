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

package io.ballerina.scanner.extension;

import org.ballerinalang.langserver.LSClientLogger;
import org.ballerinalang.langserver.commons.client.ExtendedLanguageClient;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.MessageType;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Range;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Utility methods for the Scanner Service.
 */
public class ScannerUtils {

    private static LSClientLogger clientLogger;
    private static final Set<String> previousPublishedUris = new java.util.HashSet<>();

    private ScannerUtils() {
    }

    public static void setClientLogger(LSClientLogger logger) {
        clientLogger = logger;
    }

    public static void logInfo(String msg) {
        if (clientLogger != null) {
            clientLogger.logMessage("[Scanner INFO] " + msg);
        }
    }

    public static void logError(String msg) {
        if (clientLogger != null) {
            clientLogger.logMessage("[Scanner ERROR] " + msg);
        }
    }

    public static void notifyError(String msg) {
        if (clientLogger != null) {
            clientLogger.notifyClient(MessageType.Error, "[Scanner] " + msg);
        }
    }

    /**
     * Resolve ~/.ballerina/repositories/ for the scanner JAR.
     * Invokes the Ballerina tool to locate the scanner library, checking central and
     * picking the latest version found.
     *
     * @return the scanner JAR file if found, null otherwise
     */
    public static File resolveScannerJar() {
        Process process = null;
        try {
            process = new ProcessBuilder("bal", "tool", "location", "scan", "--lib")
                    .redirectErrorStream(true)
                    .start();

            if (!process.waitFor(10, TimeUnit.SECONDS) || process.exitValue() != 0) {
                return null;
            }

            String lastNonEmptyLine = null;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (!trimmed.isEmpty()) {
                        lastNonEmptyLine = trimmed;
                    }
                }
            }

            if (lastNonEmptyLine == null) {
                return null;
            }

            return findScannerJarInLibPath(lastNonEmptyLine);
        } catch (IOException | InterruptedException | IllegalThreadStateException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return null;
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    /**
     * Locates the scanner JAR file within a given library path.
     * Prefers scan-command JAR files; falls back to any JAR in the directory.
     *
     * @param libPath the library path to search
     * @return the scanner JAR file if found, null otherwise
     */
    private static File findScannerJarInLibPath(String libPath) {
        Path libsPath;
        try {
            libsPath = Paths.get(libPath);
        } catch (Exception e) {
            return null;
        }

        File libsDirFile = libsPath.toFile();
        if (!libsDirFile.isDirectory()) {
            return null;
        }

        File[] preferredJars = libsDirFile.listFiles(
                (dir, name) -> name.startsWith("scan-command") && name.endsWith(".jar"));
        if (preferredJars != null && preferredJars.length > 0) {
            Arrays.sort(preferredJars, (x, y) -> compareJarNamesByVersion(y.getName(), x.getName()));
            return preferredJars[0];
        }

        // Accept any JAR in the directory if the preferred scanner JAR name is not present.
        File[] anyJars = libsDirFile.listFiles((dir, name) -> name.endsWith(".jar"));
        if (anyJars != null && anyJars.length > 0) {
            Arrays.sort(anyJars, (x, y) -> compareJarNamesByVersion(y.getName(), x.getName()));
            return anyJars[0];
        }

        return null;
    }

    private static int compareJarNamesByVersion(String first, String second) {
        int firstIndex = 0;
        int secondIndex = 0;

        while (firstIndex < first.length() && secondIndex < second.length()) {
            String firstPart = nextPart(first, firstIndex);
            String secondPart = nextPart(second, secondIndex);

            firstIndex += firstPart.length();
            secondIndex += secondPart.length();

            boolean firstNumeric = Character.isDigit(firstPart.charAt(0));
            boolean secondNumeric = Character.isDigit(secondPart.charAt(0));

            int comparison;
            if (firstNumeric && secondNumeric) {
                comparison = new BigInteger(firstPart).compareTo(new BigInteger(secondPart));
            } else {
                comparison = firstPart.compareTo(secondPart);
            }

            if (comparison != 0) {
                return comparison;
            }
        }

        return Integer.compare(first.length(), second.length());
    }

    private static String nextPart(String value, int startIndex) {
        int index = startIndex + 1;
        boolean digitPart = Character.isDigit(value.charAt(startIndex));

        while (index < value.length() && Character.isDigit(value.charAt(index)) == digitPart) {
            index++;
        }

        return value.substring(startIndex, index);
    }

    /**
     * Publishes diagnostics from parsed scanner issues to the language client.
     * Groups diagnostics by file URI and publishes them to VS Code.
     *
     * @param issues list of scanner issue contexts to publish
     * @param client the extended language client to send diagnostics to
     */
    public static void publishDiagnostics(
            List<ScannerIssueContext> issues,
            ExtendedLanguageClient client) {
        Map<String, List<Diagnostic>> diagnosticMap = new HashMap<>();

        for (ScannerIssueContext issue : issues) {
            String fileUri = issue.filePath;
            if (fileUri == null || fileUri.isEmpty()) {
                continue;
            }
            if (!fileUri.startsWith("file://")) {
                try {
                    fileUri = Paths.get(fileUri).toUri().toString();
                } catch (Exception e) {
                    continue;
                }
            }

            Diagnostic diagnostic = new Diagnostic();
            diagnostic.setSeverity(DiagnosticSeverity.Warning);
            diagnostic.setCode(issue.ruleId);
            diagnostic.setMessage(issue.message);
            diagnostic.setSource("Ballerina Scanner");
            diagnostic.setRange(new Range(
                    new Position(issue.startLine, issue.startColumn),
                    new Position(issue.endLine, issue.endColumn)));

            diagnosticMap
                    .computeIfAbsent(fileUri, k -> new ArrayList<>())
                    .add(diagnostic);
        }

        Set<String> currentUris = diagnosticMap.keySet();
        Set<String> staleUris = new java.util.HashSet<>(previousPublishedUris);
        staleUris.removeAll(currentUris);

        diagnosticMap.forEach((uri, diagnostics) -> {
            PublishDiagnosticsParams params =
                    new PublishDiagnosticsParams();
            params.setUri(uri);
            params.setDiagnostics(diagnostics);
            client.publishDiagnostics(params);
        });

        staleUris.forEach(uri -> {
            PublishDiagnosticsParams params = new PublishDiagnosticsParams();
            params.setUri(uri);
            params.setDiagnostics(Collections.emptyList());
            client.publishDiagnostics(params);
        });

        previousPublishedUris.clear();
        previousPublishedUris.addAll(currentUris);
    }

}
