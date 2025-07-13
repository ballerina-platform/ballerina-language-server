/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com)
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

package io.ballerina.servicemodelgenerator.extension.diagnostics;

import com.google.gson.JsonElement;
import io.ballerina.servicemodelgenerator.extension.response.ServiceDesignerDiagnosticResponse;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A class that implements a debouncer mechanism to limit the frequency of certain operations.
 *
 * @since 1.1.0
 **/
public class Debouncer {

    private static final TimeUnit TIME_UNIT = TimeUnit.MILLISECONDS;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final AtomicReference<ScheduledTaskHolder> lastFutureRef = new AtomicReference<>();

    private Debouncer() {
    }

    public CompletableFuture<ServiceDesignerDiagnosticResponse> debounce(DebouncedDiagnosticRequest request) {
        long delay = request.getDelay();
        CompletableFuture<ServiceDesignerDiagnosticResponse> future = new CompletableFuture<>();

        Future<?> scheduledFuture = scheduler.schedule(() -> {
            try {
                ServiceDesignerDiagnosticResponse result = request.call();
                future.complete(result);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        }, delay, TIME_UNIT);

        // set the last scheduled task and get the previous task if exists
        ScheduledTaskHolder previousTask = lastFutureRef.getAndSet(new ScheduledTaskHolder(future, scheduledFuture));
        // if there was a previous task, cancel it
        if (previousTask != null) {
            previousTask.future().cancel(true);
            previousTask.promise().completeExceptionally(new CancellationException("Debounced by a new request"));
        }

        return future;
    }

    public static Debouncer getInstance() {
        return Holder.INSTANCE;
    }

    private static class Holder {

        private static final Debouncer INSTANCE = new Debouncer();
    }

    /**
     * Holder for scheduled task information.
     *
     * @param promise the CompletableFuture that will eventually complete with the result of the scheduled task.
     * @param future  the Future representing the scheduled task, allowing for control over task execution.
     */
    private record ScheduledTaskHolder(CompletableFuture<?> promise, Future<?> future) {
    }
}
