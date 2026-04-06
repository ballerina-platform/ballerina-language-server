/*
 *  Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com)
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

package io.ballerina.modelgenerator.commons;

import io.ballerina.projects.Package;

import java.util.Optional;

/**
 * Compatibility facade for {@link org.ballerinalang.langserver.common.utils.PackageResolver}.
 * <p>
 * TODO: This facade exists solely to break the cyclic dependency between diagram-util and langserver-core.
 *       This should be resolved by restructuring the module dependencies in the future.
 *
 * @since 1.0.0
 */
public final class PackageUtil {

    public static Optional<Package> resolveModulePackage(String orgName, String packageName, String version) {
        return org.ballerinalang.langserver.common.utils.PackageResolver.resolveModulePackage(orgName, packageName,
                version);
    }
}
