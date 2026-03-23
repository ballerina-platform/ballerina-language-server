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

package io.ballerina.centralconnector;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.ballerina.centralconnector.response.ConnectorResponse;
import io.ballerina.centralconnector.response.ConnectorsResponse;
import io.ballerina.centralconnector.response.FunctionResponse;
import io.ballerina.centralconnector.response.FunctionsResponse;
import io.ballerina.centralconnector.response.Listener;
import io.ballerina.centralconnector.response.Listeners;
import io.ballerina.centralconnector.response.PackageResponse;
import io.ballerina.centralconnector.response.SymbolResponse;
import io.ballerina.projects.SemanticVersion;
import io.ballerina.projects.Settings;
import io.ballerina.projects.internal.model.Proxy;
import io.ballerina.projects.internal.model.Repository;
import org.ballerinalang.diagramutil.connector.models.connector.Connector;
import org.ballerinalang.maven.bala.client.MavenResolverClient;
import org.ballerinalang.maven.bala.client.MavenResolverClientException;
import org.ballerinalang.maven.bala.client.model.ConnectorSearchEntry;
import org.ballerinalang.maven.bala.client.model.ConnectorSearchMavenMetadata;
import org.ballerinalang.maven.bala.client.model.PkgSearchSolrEntry;
import org.ballerinalang.maven.bala.client.model.PkgSearchSolrMavenMetadata;
import org.ballerinalang.maven.bala.client.model.SymbolSearchEntry;
import org.ballerinalang.maven.bala.client.model.SymbolSearchMavenMetadata;
import org.wso2.ballerinalang.util.RepoUtils;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A {@code CentralAPI} implementation that delegates to a {@link MavenResolverClient} to interact with a
 * Maven-based central proxy repository.
 *
 * @since 1.0.0
 */
class MavenCentralProxyClient  {

    private static final Type listenersType = new TypeToken<List<Listener>>() { }.getType();
    private final Gson gson = new Gson();

    private final MavenResolverClient mavenResolverClient;
    private final Path cachePath;

    MavenCentralProxyClient(Repository repository, Settings settings) {
        this.mavenResolverClient = new MavenResolverClient();

        Proxy proxy = settings.getProxy();
        if (proxy != null && proxy.host() != null && !proxy.host().isEmpty()) {
            mavenResolverClient.setProxy(proxy.host(), proxy.port(), proxy.username(), proxy.password());
        }

        String username = repository.username();
        String password = repository.password();
        if (username != null && !username.isEmpty()) {
            mavenResolverClient.addRepository(repository.id(), repository.url(), username, password);
        } else {
            mavenResolverClient.addRepository(repository.id(), repository.url());
        }

        try {
            this.cachePath = Files.createTempDirectory("ballerina-central-proxy-cache");
        } catch (IOException e) {
            throw new RuntimeException("Failed to create cache directory for central proxy client", e);
        }
    }


    public PackageResponse searchPackages(Map<String, String> queryMap) {
        try {
            PkgSearchSolrMavenMetadata metadata = mavenResolverClient.getPkgSearchSolrMetadata(
                    buildQuery(queryMap), RepoUtils.getBallerinaVersion(), cachePath);
            List<PackageResponse.Package> packages = metadata.getPackages().stream()
                    .map(this::toPackage)
                    .collect(Collectors.toList());
            return new PackageResponse(packages, Collections.emptyList(), Collections.emptyMap(),
                    metadata.getCount(), metadata.getOffset(), metadata.getLimit());
        } catch (MavenResolverClientException e) {
            throw new RuntimeException("Failed to search packages via central proxy: " + e.getMessage(), e);
        }
    }


    public SymbolResponse searchSymbols(Map<String, String> queryMap) {
        try {
            SymbolSearchMavenMetadata metadata = mavenResolverClient.getSymbolSearchMetadata(
                    buildQuery(queryMap), RepoUtils.getBallerinaVersion(), cachePath);
            List<SymbolResponse.Symbol> symbols = metadata.getSymbols().stream()
                    .map(this::toSymbol)
                    .collect(Collectors.toList());
            return new SymbolResponse(symbols, metadata.getCount(), metadata.getOffset(), metadata.getLimit());
        } catch (MavenResolverClientException e) {
            throw new RuntimeException("Failed to search symbols via central proxy: " + e.getMessage(), e);
        }
    }


    public FunctionsResponse functions(String organization, String name, String version) {
        //TODO - Couldn't find any active usage. Implement if needed in the future.
        return null;
    }


    public Listeners listeners(String organization, String name, String version) {
        try {
            List<String> listenerJsonStrings = mavenResolverClient.getListeners(
                    organization, name, version, RepoUtils.getBallerinaVersion(), cachePath.toString());
            List<Listener> newListeners = new ArrayList<>();
            for (String listeners : listenerJsonStrings) {
                newListeners.addAll(gson.fromJson(listeners, listenersType));
            }
            return new Listeners(organization, name, version, newListeners);
        } catch (MavenResolverClientException e) {
            throw new RuntimeException("Failed to get listeners via central proxy: " + e.getMessage(), e);
        }
    }


    public FunctionResponse function(String organization, String name, String version, String functionName) {
        //TODO - Couldn't find any active usage. Implement if needed in the future.
        return null;
    }


    public ConnectorsResponse connectors(Map<String, String> queryMap) {
        try {
            ConnectorSearchMavenMetadata metadata = mavenResolverClient.getConnectorSearchMetadata(
                    buildQuery(queryMap), RepoUtils.getBallerinaVersion(), cachePath);
            List<Connector> connectors = metadata.getConnectors().stream()
                    .map(this::toConnector)
                    .collect(Collectors.toList());
            return new ConnectorsResponse(connectors, metadata.getCount(), metadata.getOffset(), metadata.getLimit());
        } catch (MavenResolverClientException e) {
            throw new RuntimeException("Failed to search connectors via central proxy: " + e.getMessage(), e);
        }
    }


    public ConnectorResponse connector(String id) {
        //TODO - Couldn't find any active usage. Implement if needed in the future.
        return null;
    }


    public ConnectorResponse connector(String organization, String name, String version, String clientName) {
        //TODO - Couldn't find any active usage. Implement if needed in the future.
        return null;
    }


    public String latestPackageVersion(String org, String name) {
        try {
            List<String> versions = mavenResolverClient.getPackageVersionsInCentralProxy(org, name,
                    RepoUtils.getBallerinaVersion(), cachePath);
            if (versions.isEmpty()) {
                throw new RuntimeException("No versions found for the package: " + org + "/" + name);
            }
            String latestVersion = versions.getFirst();
            for (String version : versions) {
                if (SemanticVersion.from(version).greaterThan(SemanticVersion.from(latestVersion))) {
                    latestVersion = version;
                }
            }
            return latestVersion;
        } catch (MavenResolverClientException e) {
            throw new RuntimeException("Package versions cannot be pulled: " + e.getMessage(), e);
        }
    }


    public boolean hasAuthorizedAccess() {
        return false;
    }

    private String buildQuery(Map<String, String> queryMap) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : queryMap.entrySet()) {
            if (!sb.isEmpty()) {
                sb.append("&");
            }
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        return sb.toString();
    }

    private PackageResponse.Package toPackage(PkgSearchSolrEntry entry) {
        return new PackageResponse.Package(
                (int) entry.getId(),
                entry.getOrg(),
                entry.getName(),
                entry.getVersion(),
                null, null, false, null, null, null, null, null,
                entry.getSummary(),
                null, false,
                Collections.emptyList(),
                entry.getAuthors() != null ? entry.getAuthors() : Collections.emptyList(),
                null,
                entry.getKeywords() != null ? entry.getKeywords() : Collections.emptyList(),
                null, null, null,
                entry.getCreatedDate(),
                (int) entry.getPullCount(), null,
                Collections.emptyList(),
                null, null
        );
    }

    private SymbolResponse.Symbol toSymbol(SymbolSearchEntry entry) {
        return new SymbolResponse.Symbol(
                entry.getId(),
                entry.getPackageID(),
                entry.getName(),
                entry.getOrg(),
                entry.getVersion(),
                entry.getCreatedDate(),
                entry.getIcon(),
                entry.getSymbolType(),
                entry.getSymbolParent(),
                entry.getSymbolName(),
                entry.getDescription(),
                entry.getSymbolSignature(),
                entry.isIsolated(),
                entry.isRemote(),
                entry.isResource(),
                entry.isClosed(),
                entry.isDistinct(),
                entry.isReadOnly()
        );
    }

    private Connector toConnector(ConnectorSearchEntry entry) {
        Connector c = new Connector(
                entry.getPackageInfo().getOrganization(),
                entry.getModuleName(),
                entry.getPackageInfo().getName(),
                entry.getPackageInfo().getVersion(),
                entry.getName(),
                entry.getDocumentation(),
                null
        );
        c.id = entry.getId();
        c.icon = entry.getIcon();
        return c;
    }
}
