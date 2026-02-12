/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com)
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.scanner.extension.codeaction;

import org.eclipse.lsp4j.TextEdit;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single code action (quick-fix) that can be offered
 * to the user for a scanner diagnostic.
 */
public class ScannerCodeAction {

    /**
     * Human-readable title shown in the lightbulb menu.
     */
    private String title;

    /**
     * Code action kind, e.g. "quickfix" or "refactor".
     */
    private String kind;

    /**
     * The text edits to apply when the user selects this action.
     * Uses the standard LSP {@link TextEdit} type.
     */
    private List<TextEdit> edits;

    public ScannerCodeAction() {
        this.edits = new ArrayList<>();
    }

    public ScannerCodeAction(String title, String kind, List<TextEdit> edits) {
        this.title = title;
        this.kind = kind;
        this.edits = (edits != null) ? new ArrayList<>(edits) : new ArrayList<>();
    }

    // --- Getters and Setters ---

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public List<TextEdit> getEdits() {
        return new ArrayList<>(this.edits);
    }

    public void setEdits(List<TextEdit> edits) {
        this.edits = (edits != null) ? new ArrayList<>(edits) : new ArrayList<>();
    }
}
