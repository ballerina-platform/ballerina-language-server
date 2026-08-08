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

/**
 * Data Transfer Object for sending scanner security issues to the Language Client.
 * Contains issue metadata (rule, severity, kind) and location information.
 */
public class ScannerIssueContext {
    /** Unique identifier for the security rule. */
    public String ruleId;
    /** Human-readable message describing the issue. */
    public String message;
    /** Severity level of the issue (e.g., ERROR, WARNING). */
    public String severity;
    /** Type/category of the rule (e.g., SEMANTIC, STYLE). */
    public String ruleKind;
    /** File path where the issue is located. */
    public String filePath;

    // Location Data
    /** Starting line number (0-based). */
    public int startLine;
    /** Starting column number (0-based). */
    public int startColumn;
    /** Ending line number (0-based). */
    public int endLine;
    /** Ending column number (0-based). */
    public int endColumn;

    @Override
    public String toString() {
        return "ScannerIssueContext{" +
                "ruleId='" + ruleId + '\'' +
                ", severity='" + severity + '\'' +
                ", ruleKind='" + ruleKind + '\'' +
                ", message='" + message + '\'' +
                ", filePath='" + filePath + '\'' +
                ", range=" + startLine + ":" + startColumn + "-" + endLine + ":" + endColumn +
                '}';
    }
}
