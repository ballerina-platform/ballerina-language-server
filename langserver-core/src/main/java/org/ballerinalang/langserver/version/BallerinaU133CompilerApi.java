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

package org.ballerinalang.langserver.version;

import io.ballerina.projects.Settings;
import io.ballerina.projects.internal.model.Repository;
import org.ballerinalang.annotation.JavaSPIService;
import org.wso2.ballerinalang.util.RepoUtils;

/**
 * Compiler API implementation for Ballerina 2201.13.3.
 *
 * @since 1.0.0
 */
@JavaSPIService("org.ballerinalang.langserver.commons.BallerinaCompilerApi")
public class BallerinaU133CompilerApi extends BallerinaU130CompilerApi {

    @Override
    public String getVersion() {
        return "2201.13.3";
    }

    @Override
    public  boolean isCentralProxyEnabled() {
        Settings settings = RepoUtils.readSettings();
        Repository[] repositories = settings.getRepositories();
        if (repositories != null) {
            for (Repository repo : repositories) {
                if (repo.proxyCentral()) {
                    return true;
                }
            }
        }
        return false;
    }
}
