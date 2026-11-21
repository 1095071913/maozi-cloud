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
 * 处理 {@link CompletableFuture} 的工具类
 *
 * @author Eric Zhao
 * @date 2026/11/22
 * @since 1.4.1
 */
public final class AsyncUtils {

    /** 等待异步结果失败时使用的日志记录器 */
    private static final Logger LOG = LoggerFactory.getLogger(AsyncUtils.class);

    /** 等待单个异步结果的超时时间（秒） */
    private static final long GET_VALUE_TIMEOUT_SECONDS = 10L;

    /** 返回一个以给定异常完成的失败 Future */
    public static <R> CompletableFuture<R> newFailedFuture(Throwable ex) {
        CompletableFuture<R> future = new CompletableFuture<>();
        future.completeExceptionally(ex);
        return future;
    }

    /**
     * 将给定 Future 列表合并为一个 Future：全部成功完成时以非空结果列表完成，
     * 任一 Future 失败时以异常完成
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
     * 合并给定 Future 列表并忽略失败：并行等待每个 Future（单个最多 10 秒），
     * 以成功 Future 的非空结果列表完成
     */
    public static <R> CompletableFuture<List<R>> sequenceSuccessFuture(List<CompletableFuture<R>> futures) {
        return CompletableFuture.supplyAsync(() -> futures.parallelStream()
            .map(AsyncUtils::getValue)
            .filter(Objects::nonNull)
            .collect(Collectors.toList())
        );
    }

    /**
     * 最多等待 10 秒获取给定 Future 的结果，等待或 Future 失败时
     * 返回 null 并记录错误日志
     */
    public static <T> T getValue(CompletableFuture<T> future) {
        try {
            return future.get(GET_VALUE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            LOG.error("getValue for async result failed", ex);
        } catch (Exception ex) {
            LOG.error("getValue for async result failed", ex);
        }
        return null;
    }

    /** 判断 Future 是否已完成且正常完成（非异常完成、非取消） */
    public static boolean isSuccessFuture(CompletableFuture<?> future) {
        return future.isDone() && !future.isCompletedExceptionally() && !future.isCancelled();
    }

    /** 私有构造方法，防止实例化 */
    private AsyncUtils() {}
}
