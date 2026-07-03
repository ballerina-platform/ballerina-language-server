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

package io.ballerina.scanner.extension.response;

import io.ballerina.scanner.extension.ScannerExclusionContext;
import io.ballerina.scanner.extension.ScannerIssueContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Response object containing scanner vulnerability scan results.
 * Includes both active issues found and previously excluded issues.
 */
public class ScanResponse extends BaseResponse {

    private List<ScannerIssueContext> activeIssues;
    private List<ScannerExclusionContext> excludedIssues;
    private boolean dependentPackageIssuesFound;

    public ScanResponse() {
        this.activeIssues = new ArrayList<>();
        this.excludedIssues = new ArrayList<>();
    }

    public List<ScannerIssueContext> getActiveIssues() {
        return new ArrayList<>(this.activeIssues);
    }

    public void setActiveIssues(List<ScannerIssueContext> activeIssues) {
        this.activeIssues = (activeIssues != null) ? new ArrayList<>(activeIssues) : new ArrayList<>();
    }

    public List<ScannerExclusionContext> getExcludedIssues() {
        return new ArrayList<>(this.excludedIssues);
    }

    public void setExcludedIssues(List<ScannerExclusionContext> excludedIssues) {
        this.excludedIssues = (excludedIssues != null) ? new ArrayList<>(excludedIssues) : new ArrayList<>();
    }

    public boolean isDependentPackageIssuesFound() {
        return dependentPackageIssuesFound;
    }

    public void setDependentPackageIssuesFound(boolean dependentPackageIssuesFound) {
        this.dependentPackageIssuesFound = dependentPackageIssuesFound;
    }
}
