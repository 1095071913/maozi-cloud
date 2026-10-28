/*
 * Copyright 1999-2018 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.csp.sentinel.dashboard.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Utility class for handling {@link CompletableFuture}s.
 *
 * @author Eric Zhao
 * @since 1.4.1
 */
public final class AsyncUtils {

    /** Logger used when waiting for async results fails. */
    private static final Logger LOG = LoggerFactory.getLogger(AsyncUtils.class);

    /** Returns a future already completed exceptionally with the given throwable. */
    public static <R> CompletableFuture<R> newFailedFuture(Throwable ex) {
        CompletableFuture<R> future = new CompletableFuture<>();
        future.completeExceptionally(ex);
        return future;
    }

    /**
     * Combines the given futures into one future that completes with the list of all
     * non-null values once every future has completed successfully; it completes
     * exceptionally if any of the futures fails.
     */
    public static <R> CompletableFuture<List<R>> sequenceFuture(List<CompletableFuture<R>> futures) {
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(AsyncUtils::getValue)
                .filter(Objects::nonNull)
                .collect(Collectors.toList())
            );
    }

    /**
     * Combines the given futures while ignoring failures: waits for each future in
     * parallel (up to 10 seconds each) and completes with the list of non-null results
     * of the futures that succeeded.
     */
    public static <R> CompletableFuture<List<R>> sequenceSuccessFuture(List<CompletableFuture<R>> futures) {
        return CompletableFuture.supplyAsync(() -> futures.parallelStream()
            .map(AsyncUtils::getValue)
            .filter(Objects::nonNull)
            .collect(Collectors.toList())
        );
    }

    /**
     * Waits up to 10 seconds for the result of the given future, returning null and
     * logging the error when the wait or the future fails.
     */
    public static <T> T getValue(CompletableFuture<T> future) {
        try {
            return future.get(10, TimeUnit.SECONDS);
        } catch (Exception ex) {
            LOG.error("getValue for async result failed", ex);
        }
        return null;
    }

    /** Returns whether the future is done and completed normally (not exceptionally and not cancelled). */
    public static boolean isSuccessFuture(CompletableFuture future) {
        return future.isDone() && !future.isCompletedExceptionally() && !future.isCancelled();
    }

    /** Private constructor to prevent instantiation. */
    private AsyncUtils() {}
}
