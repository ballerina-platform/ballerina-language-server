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
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Range;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Utility methods for the Scanner Service.
 */
public class ScannerUtils {

    private ScannerUtils() {
    }

    // Logging Helpers
    public static void logInfo(LSClientLogger logger, String msg) {
        if (logger != null) {
            logger.logMessage("[Scanner INFO] " + msg);
        }
    }

    public static void logError(LSClientLogger logger, String msg) {
        if (logger != null) {
            logger.logMessage("[Scanner ERROR] " + msg);
        }
    }

    /**
     * Searches ~/.ballerina/repositories/ for the scan-command JAR.
     * Checks central. Picks the latest version found.
     */
    public static File resolveScannerJar() {
        File scannerJar = resolveScannerJarFromToolLocation();
        if (scannerJar != null) {
            return scannerJar;
        }

        return resolveScannerJarFromRepository();
    }

    private static File resolveScannerJarFromToolLocation() {
        Process process = null;
        try {
            process = new ProcessBuilder("bal", "tool", "location", "scan", "--lib")
                    .redirectErrorStream(true)
                    .start();

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

            if (!process.waitFor(10, TimeUnit.SECONDS) || process.exitValue() != 0 || lastNonEmptyLine == null) {
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
            Arrays.sort(preferredJars, (a, b) -> b.getName().compareTo(a.getName()));
            return preferredJars[0];
        }

        // Fallback to any jar in the libs path to tolerate future scanner jar name changes.
        File[] anyJars = libsDirFile.listFiles((dir, name) -> name.endsWith(".jar"));
        if (anyJars != null && anyJars.length > 0) {
            Arrays.sort(anyJars, (a, b) -> b.getName().compareTo(a.getName()));
            return anyJars[0];
        }

        return null;
    }

    private static File resolveScannerJarFromRepository() {
        Path balHome = Paths.get(System.getProperty("user.home"), ".ballerina", "repositories");

        // Search central repo
        String[] repos = {"central.ballerina.io"};
        Path toolBase = Paths.get("bala", "ballerina", "tool_scan");

        for (String repo : repos) {
            Path toolDir = balHome.resolve(repo).resolve(toolBase);
            File toolDirFile = toolDir.toFile();
            if (!toolDirFile.isDirectory()) {
                continue;
            }

            // List version directories and pick the latest
            File[] versionDirs = toolDirFile.listFiles(File::isDirectory);
            if (versionDirs == null || versionDirs.length == 0) {
                continue;
            }

            // Sort descending to get latest version first
            Arrays.sort(versionDirs, (a, b) -> b.getName().compareTo(a.getName()));

            for (File versionDir : versionDirs) {
                // Search all platform directories
                File[] platformDirs = versionDir.listFiles(File::isDirectory);
                if (platformDirs == null) {
                    continue;
                }

                for (File platformDir : platformDirs) {
                    Path libsDir = platformDir.toPath().resolve("tool").resolve("libs");
                    File libsDirFile = libsDir.toFile();
                    if (!libsDirFile.isDirectory()) {
                        continue;
                    }

                    File scannerJar = findScannerJarInLibPath(libsDir.toString());
                    if (scannerJar != null) {
                        return scannerJar;
                    }
                }
            }
        }

        // Return null if not found
        return null;
    }

    /**
     * Publishes diagnostics from parsed ScannerIssueContext list.
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

        diagnosticMap.forEach((uri, diagnostics) -> {
            PublishDiagnosticsParams params =
                    new PublishDiagnosticsParams();
            params.setUri(uri);
            params.setDiagnostics(diagnostics);
            client.publishDiagnostics(params);
        });
    }

}
