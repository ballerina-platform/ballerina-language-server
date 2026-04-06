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

import io.ballerina.centralconnector.CentralAPI;
import io.ballerina.centralconnector.RemoteCentral;
import io.ballerina.projects.Package;
import io.ballerina.projects.PackageDescriptor;
import io.ballerina.projects.PackageName;
import io.ballerina.projects.PackageOrg;
import io.ballerina.projects.PackageVersion;
import io.ballerina.projects.environment.PackageMetadataResponse;
import io.ballerina.projects.environment.ResolutionOptions;
import io.ballerina.projects.environment.ResolutionRequest;
import io.ballerina.projects.environment.ResolutionResponse;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

/**
 * Online implementation of {@link PackageResolver} that resolves packages by contacting
 * the Ballerina central repository. When a package or its latest version is not
 * available locally, it fetches the metadata and downloads the package from central.
 *
 * @since 1.7.0
 */
final class RemotePackageResolver extends PackageResolver {

    @Override
    protected Optional<Package> getModulePackage(String org, String name, String version) {
        ResolutionRequest resolutionRequest = ResolutionRequest.from(
                PackageDescriptor.from(PackageOrg.from(org), PackageName.from(name), PackageVersion.from(version)));

        io.ballerina.projects.environment.PackageResolver resolver =
                sampleProject().projectEnvironmentContext()
                        .getService(io.ballerina.projects.environment.PackageResolver.class);
        Collection<ResolutionResponse> resolutionResponses = resolver.resolvePackages(
                Collections.singletonList(resolutionRequest),
                ResolutionOptions.builder().setOffline(false).setSticky(false).build());
        return resolutionResponses.stream().findFirst()
                .flatMap(response -> loadBalaPackage(response.resolvedPackage().project().sourceRoot()));
    }

    @Override
    protected Optional<Package> getModulePackage(String org, String name) {
        ResolutionRequest resolutionRequest = ResolutionRequest.from(
                PackageDescriptor.from(PackageOrg.from(org), PackageName.from(name)));
        io.ballerina.projects.environment.PackageResolver packageResolver =
                sampleProject().projectEnvironmentContext()
                        .getService(io.ballerina.projects.environment.PackageResolver.class);
        Collection<PackageMetadataResponse> packageMetadataResponses = packageResolver.resolvePackageMetadata(
                Collections.singletonList(resolutionRequest),
                ResolutionOptions.builder().setOffline(true).build());
        Optional<PackageMetadataResponse> pkgMetadata = packageMetadataResponses.stream().findFirst();
        PackageDescriptor packageDescriptor;
        if (pkgMetadata.isEmpty() ||
                pkgMetadata.get().resolutionStatus() == ResolutionResponse.ResolutionStatus.UNRESOLVED) {
            CentralAPI centralApi = RemoteCentral.getInstance();
            String version = centralApi.latestPackageVersion(org, name);
            packageDescriptor = PackageDescriptor.from(
                    PackageOrg.from(org), PackageName.from(name), PackageVersion.from(version));
        } else {
            packageDescriptor = pkgMetadata.get().resolvedDescriptor();
        }

        Collection<ResolutionResponse> resolutionResponses = packageResolver.resolvePackages(
                Collections.singletonList(ResolutionRequest.from(packageDescriptor)),
                ResolutionOptions.builder().setOffline(false).build());
        return resolutionResponses.stream().findFirst()
                .flatMap(response -> loadBalaPackage(response.resolvedPackage().project().sourceRoot()));
    }

    @Override
    protected boolean isModuleUnresolvedInternal(String org, String name, String version) {
        ResolutionRequest resolutionRequest = ResolutionRequest.from(
                PackageDescriptor.from(PackageOrg.from(org), PackageName.from(name), PackageVersion.from(version)));
        io.ballerina.projects.environment.PackageResolver packageResolver =
                sampleProject().projectEnvironmentContext()
                        .getService(io.ballerina.projects.environment.PackageResolver.class);
        return packageResolver.resolvePackageMetadata(Collections.singletonList(resolutionRequest),
                        ResolutionOptions.builder().setOffline(true).build()).stream()
                .findFirst()
                .map(response -> response.resolutionStatus() == ResolutionResponse.ResolutionStatus.UNRESOLVED)
                .orElse(false);
    }

    @Override
    protected String fetchVersionIfNotExistsInternal(String org, String packageName, String version) {
        if (version == null) {
            CentralAPI centralApi = RemoteCentral.getInstance();
            return centralApi.latestPackageVersion(org, packageName);
        }
        return version;
    }
}
