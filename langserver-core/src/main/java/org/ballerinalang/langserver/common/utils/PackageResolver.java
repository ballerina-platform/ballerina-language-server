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

package org.ballerinalang.langserver.common.utils;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.projects.Module;
import io.ballerina.projects.ModuleId;
import io.ballerina.projects.ModuleName;
import io.ballerina.projects.Package;
import io.ballerina.projects.PackageCompilation;
import io.ballerina.projects.PackageDescriptor;
import io.ballerina.projects.PackageName;
import io.ballerina.projects.Project;
import io.ballerina.projects.ProjectEnvironmentBuilder;
import io.ballerina.projects.bala.BalaProject;
import io.ballerina.projects.directory.BuildProject;
import io.ballerina.projects.repos.TempDirCompilationCache;
import org.ballerinalang.langserver.LSClientLogger;
import org.ballerinalang.langserver.commons.eventsync.exceptions.EventSyncException;
import org.ballerinalang.langserver.commons.workspace.WorkspaceDocumentException;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;
import org.eclipse.lsp4j.MessageType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Provides utility methods for resolving, loading, and compiling Ballerina packages and modules
 * within the language server. This class acts as a singleton, initialized in either online or
 * offline mode, and delegates package resolution to the appropriate subclass.
 *
 * <p>Online mode ({@link RemotePackageResolver}) resolves packages via the Ballerina central
 * repository, while offline mode ({@link OfflinePackageResolver}) resolves only from the local
 * cache.</p>
 *
 * @since 1.7.0
 */
public abstract class PackageResolver {

    private static final String BALLERINA_HOME_PROPERTY = "ballerina.home";
    private static final String PULLING_THE_MODULE_MESSAGE = "Pulling the module '%s' from the central";
    private static final String MODULE_PULLING_FAILED_MESSAGE = "Failed to pull the module: %s";
    private static final String MODULE_PULLING_SUCCESS_MESSAGE = "Successfully pulled the module: %s";

    private static final ConcurrentHashMap<Path, ReentrantLock> PROJECT_LOCKS = new ConcurrentHashMap<>();

    private static volatile PackageResolver instance;

    private final BuildProject sampleProject;

    protected PackageResolver() {
        this.sampleProject = createSampleProject();
    }

    /**
     * Initializes the package utility singleton.
     *
     * @param offline Whether the utility should resolve packages in offline mode.
     */
    public static synchronized void initialize(boolean offline) {
        instance = offline ? new OfflinePackageResolver() : new RemotePackageResolver();
    }

    public static BuildProject getSampleProject() {
        return instance.sampleProject;
    }

    public static Optional<SemanticModel> getSemanticModel(String org, String packageName, String moduleName,
                                                           String version) {
        Optional<Package> modulePackage = instance.getModulePackage(org, packageName, version);
        if (modulePackage.isEmpty()) {
            return Optional.empty();
        }
        Package pkg = modulePackage.get();
        for (Module module : pkg.modules()) {
            if (module.moduleName().toString().equals(moduleName)) {
                return Optional.of(getCompilation(pkg).getSemanticModel(module.moduleId()));
            }
        }
        return Optional.empty();
    }

    public static Optional<SemanticModel> getSemanticModel(String org, String name) {
        return instance.getModulePackage(org, name).map(
                pkg -> getCompilation(pkg).getSemanticModel(pkg.getDefaultModule().moduleId()));
    }

    public static boolean isModuleUnresolved(String org, String name, String version) {
        return instance.isModuleUnresolvedInternal(org, name, version);
    }

    private static Path getPath(Path path) {
        return Objects.requireNonNull(path, "Path cannot be null");
    }

    private static Path getParentPath(Path path) {
        return Objects.requireNonNull(path, "Path cannot be null").getParent();
    }

    public static Project loadProject(WorkspaceManager workspaceManager, Path filePath) {
        try {
            return workspaceManager.loadProject(filePath);
        } catch (WorkspaceDocumentException | EventSyncException e) {
            throw new RuntimeException("Error loading project: " + e.getMessage());
        }
    }

    public static Optional<SemanticModel> getSemanticModelIfMatched(WorkspaceManager workspaceManager, Path filePath,
                                                                    String orgName, String packageName,
                                                                    String modulePartName, String version) {
        try {
            Project project = workspaceManager.loadProject(filePath);
            Package currentPackage = project.currentPackage();
            PackageDescriptor descriptor = currentPackage.descriptor();
            if (descriptor.org().value().equals(orgName) &&
                    descriptor.name().value().equals(packageName) &&
                    descriptor.version().value().toString().equals(version)) {
                ModuleId moduleId = currentPackage.getDefaultModule().moduleId();
                if (Objects.nonNull(modulePartName) && !modulePartName.isEmpty()
                        && !packageName.equals(modulePartName)) {
                    ModuleName subModuleName = ModuleName.from(PackageName.from(packageName), modulePartName);
                    Module module = currentPackage.module(subModuleName);
                    if (module == null) {
                        for (Module mod : currentPackage.modules()) {
                            if (mod.moduleName().toString().equals(modulePartName)) {
                                module = mod;
                                break;
                            }
                        }
                        if (module == null) {
                            return Optional.empty();
                        }
                    }
                    moduleId = module.moduleId();
                }
                return Optional.of(PackageResolver.getCompilation(currentPackage).getSemanticModel(moduleId));
            }
        } catch (WorkspaceDocumentException | EventSyncException e) {
            // Ignore and fall through to Optional.empty().
        }
        return Optional.empty();
    }

    public static String fetchVersionIfNotExists(String org, String packageName, String version) {
        return instance.fetchVersionIfNotExistsInternal(org, packageName, version);
    }

    public static Optional<Package> pullModuleAndNotify(LSClientLogger lsClientLogger, String org, String packageName,
                                                        String moduleName, String version) {
        return instance.pullModuleAndNotifyInternal(lsClientLogger, org, packageName, moduleName, version);
    }

    private static void notifyClient(LSClientLogger lsClientLogger, String org, String packageName, String version,
                                     MessageType messageType, String message) {
        if (lsClientLogger != null) {
            String signature = String.format("%s/%s:%s", org, packageName, version);
            lsClientLogger.notifyClient(messageType, String.format(message, signature));
        }
    }

    public static PackageCompilation getCompilation(Package balPackage) {
        Path id = balPackage.project().sourceRoot();
        ReentrantLock lock = PROJECT_LOCKS.computeIfAbsent(id, k -> new ReentrantLock());
        lock.lock();
        try {
            return balPackage.getCompilation();
        } finally {
            lock.unlock();
        }
    }

    public static PackageCompilation getCompilation(Project project) {
        return getCompilation(project.currentPackage());
    }

    public static Optional<Package> resolveModulePackage(String org, String packageName, String version) {
        try {
            if (version == null) {
                return instance.getModulePackage(org, packageName);
            } else {
                return instance.getModulePackage(org, packageName, version);
            }
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public static boolean isLocalFunction(WorkspaceManager workspaceManager, Path filePath, String org,
                                          String moduleName) {
        if (org == null || moduleName == null) {
            return false;
        }
        try {
            Project project = workspaceManager.loadProject(filePath);
            PackageDescriptor descriptor = project.currentPackage().descriptor();
            String packageOrg = descriptor.org().value();
            String packageName = descriptor.name().value();

            return packageOrg.equals(org) && packageName.equals(moduleName);
        } catch (WorkspaceDocumentException | EventSyncException e) {
            return false;
        }
    }

    protected abstract Optional<Package> getModulePackage(String org, String name, String version);

    protected abstract Optional<Package> getModulePackage(String org, String name);

    protected abstract boolean isModuleUnresolvedInternal(String org, String name, String version);

    protected abstract String fetchVersionIfNotExistsInternal(String org, String packageName, String version);

    protected BuildProject sampleProject() {
        return sampleProject;
    }

    private Optional<Package> pullModuleAndNotifyInternal(LSClientLogger lsClientLogger, String org,
                                                          String packageName, String moduleName, String version) {
        String resolvedVersion = fetchVersionIfNotExistsInternal(org, packageName, version);
        if (resolvedVersion == null) {
            return Optional.empty();
        }

        Optional<Package> modulePackage;
        if (isModuleUnresolvedInternal(org, packageName, resolvedVersion)) {
            notifyClient(lsClientLogger, org, packageName, resolvedVersion, MessageType.Info,
                    PULLING_THE_MODULE_MESSAGE);
            modulePackage = getModulePackage(org, packageName, resolvedVersion);
            if (modulePackage.isEmpty()) {
                notifyClient(lsClientLogger, org, packageName, resolvedVersion, MessageType.Error,
                        MODULE_PULLING_FAILED_MESSAGE);
            } else {
                notifyClient(lsClientLogger, org, packageName, resolvedVersion, MessageType.Info,
                        MODULE_PULLING_SUCCESS_MESSAGE);
            }
        } else {
            modulePackage = getModulePackage(org, packageName, resolvedVersion);
        }
        return modulePackage;
    }

    protected Optional<Package> loadBalaPackage(Path balaPath) {
        ProjectEnvironmentBuilder defaultBuilder = ProjectEnvironmentBuilder.getDefaultBuilder();
        defaultBuilder.addCompilationCacheFactory(TempDirCompilationCache::from);
        BalaProject balaProject = BalaProject.loadProject(defaultBuilder, balaPath);
        return Optional.ofNullable(balaProject.currentPackage());
    }

    private static BuildProject createSampleProject() {
        String ballerinaHome = System.getProperty(BALLERINA_HOME_PROPERTY);
        if (ballerinaHome == null || ballerinaHome.isEmpty()) {
            Path currentPath = getPath(Paths.get(
                    PackageResolver.class.getProtectionDomain().getCodeSource().getLocation().getPath()));
            Path distributionPath = getParentPath(getParentPath(getParentPath(currentPath)));
            System.setProperty(BALLERINA_HOME_PROPERTY, distributionPath.toString());
        }

        try {
            Path tempDir = Files.createTempDirectory("ballerina-sample");
            Files.createFile(tempDir.resolve("main.bal"));

            Path ballerinaTomlFile = tempDir.resolve("Ballerina.toml");
            String tomlContent = "[package]\n" +
                    "org = \"wso2\"\n" +
                    "name = \"sample\"\n" +
                    "version = \"0.1.0\"\n" +
                    "distribution = \"2201.12.0\"";
            Files.writeString(ballerinaTomlFile, tomlContent, StandardOpenOption.CREATE);
            return BuildProject.load(tempDir);
        } catch (IOException e) {
            throw new RuntimeException("Error occurred while creating the sample project", e);
        }
    }
}
