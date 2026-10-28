package com.maozi.lock.error.strategy.impl;

import com.maozi.common.result.error.code.ErrorCode;
import com.maozi.common.result.error.code.SystemErrorCode;
import com.maozi.common.result.error.exception.BusinessResultException;
import com.maozi.lock.error.strategy.LockTimeoutHandler;
import com.maozi.lock.lock.Lock;
import lombok.SneakyThrows;

import java.util.concurrent.TimeUnit;

/**
 * 加锁超时策略枚举
 * <p>
 * 定义两种加锁超时处理策略：
 * <ul>
 *   <li>FAIL_FAST - 快速失败，直接抛出限流异常</li>
 *   <li>KEEP_ACQUIRE - 持续重试获取锁，重试间隔指数翻倍，间隔超过上限后失败</li>
 * </ul>
 * </p>
 *
 * @author maozi
 */
public enum LockTimeoutStrategy implements LockTimeoutHandler {

    /** 快速失败策略 */
    FAIL_FAST() {

        /**
         * 直接抛出限流异常
         */
        @Override
        public void handle(String key,Long waitTime,Long leaseTime,Lock lock) {
            ErrorCode errorCode = SystemErrorCode.CURRENT_LIMITING_ERROR;
            throw new BusinessResultException(errorCode).setHttpCode(errorCode.getCode());
        }

    },

    /** 持续获取锁策略 */
    KEEP_ACQUIRE() {

        /** 默认重试间隔 100ms */
        private static final Long DEFAULT_INTERVAL = 100L;

        /** 默认最大重试间隔 3 分钟（重试间隔超过该阈值后停止重试并抛出限流异常） */
        private static final Long DEFAULT_MAX_INTERVAL = 3L * 60L * 1000L;

        /**
         * 以指数退避方式持续重试获取锁，重试间隔翻倍增长、超过上限后抛出限流异常
         */
        @Override
        @SneakyThrows
        public void handle(String key,Long waitTime,Long leaseTime,Lock lock){

            // 初始重试间隔 100ms
            Long interval = DEFAULT_INTERVAL;

            // 持续尝试获取锁，直到成功或重试间隔超过上限
            while (!lock.lock(key,waitTime,leaseTime)) {

                // 如果当前重试间隔超过上限（3分钟），则停止重试并抛出限流异常
                if (interval > DEFAULT_MAX_INTERVAL) {
                    ErrorCode errorCode = SystemErrorCode.CURRENT_LIMITING_ERROR;
                    throw new BusinessResultException(errorCode).setHttpCode(errorCode.getCode());
                }

                // 按当前间隔休眠后重试
                TimeUnit.MILLISECONDS.sleep(interval);

                // 指数退避：每次重试间隔翻倍（100ms -> 200ms -> 400ms -> ...）
                interval <<= 1;

            }

        }

    }

}
