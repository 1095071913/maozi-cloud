package com.maozi.lock.error.strategy.impl;

import com.maozi.common.result.error.code.ErrorCode;
import com.maozi.common.result.error.code.SystemErrorCode;
import com.maozi.common.result.error.exception.BusinessResultException;
import com.maozi.lock.error.strategy.UnLockTimeoutHandler;

/**
 * 解锁超时策略枚举
 * <p>
 * 定义两种解锁超时处理策略：
 * <ul>
 *   <li>NO_OPERATION - 不做任何处理</li>
 *   <li>FAIL_FAST - 快速失败，抛出限流异常</li>
 * </ul>
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
public enum UnLockTimeoutStrategy implements UnLockTimeoutHandler {

    /** 不做任何处理 */
    NO_OPERATION() {

        /**
         * 解锁超时时不执行任何操作（空实现，既不记录日志也不抛异常）
         */
        @Override
        public void handle() {}

    },

    /** 快速失败策略 */
    FAIL_FAST() {

        /**
         * 抛出限流异常
         */
        @Override
        public void handle() {
            ErrorCode errorCode = SystemErrorCode.CURRENT_LIMITING_ERROR;
            throw new BusinessResultException(errorCode).setHttpCode(errorCode.getCode());
        }

    }

}
