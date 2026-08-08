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

package io.ballerina.scanner.extension.request;

/**
 * Request object for scanner service vulnerability scan operations.
 * Contains the document URI and build configuration options.
 */
public class ScanRequest {

    private String documentUri;

    /**
     * If true, the build will not attempt to download dependencies from the internet.
     * Default: false
     */
    private boolean offline = false;

    /**
     * If true, the build will use the versions locked in Dependencies.toml
     * instead of checking for newer versions.
     * Default: true
     */
    private boolean sticky = true;

    /**
     * If true, compilation of test files is skipped to save time.
     * Default: true
     */
    private boolean skipTests = true;

    /**
     * If true, the scanner is allowed to write files to the disk
     * (e.g. creating Dependencies.toml after a successful resolution).
     * Set to 'false' if want a Read-Only scan.
     * Default: true
     */
    private boolean saveProject = true;

    /**
     * If true, publish Diagnostics to the VS Code.
     * Default: true
     */
    private boolean publishDiagnostics = true;

    public String getDocumentUri() {
        return documentUri;
    }

    public void setDocumentUri(String documentUri) {
        this.documentUri = documentUri;
    }

    public boolean isOffline() {
        return offline;
    }

    public void setOffline(boolean offline) {
        this.offline = offline;
    }

    public boolean isSticky() {
        return sticky;
    }

    public void setSticky(boolean sticky) {
        this.sticky = sticky;
    }

    public boolean isSkipTests() {
        return skipTests;
    }

    public void setSkipTests(boolean skipTests) {
        this.skipTests = skipTests;
    }

    public boolean isSaveProject() {
        return saveProject;
    }

    public void setSaveProject(boolean saveProject) {
        this.saveProject = saveProject;
    }

    public boolean isPublishDiagnostics() {
        return publishDiagnostics;
    }

    public void setPublishDiagnostics(boolean publishDiagnostics) {
        this.publishDiagnostics = publishDiagnostics;
    }
}
