package com.maozi.lock.error.strategy;

/**
 * 解锁超时处理策略接口
 * <p>
 * 定义解锁超时时的处理行为，由具体策略实现。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
public interface UnLockTimeoutHandler {

    /**
     * 处理解锁超时
     */
    void handle();

}
