package com.maozi.lock.error.strategy;

import com.maozi.lock.lock.Lock;

/**
 * 加锁超时处理策略接口
 * <p>
 * 定义加锁超时时的处理行为，由具体策略实现。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
public interface LockTimeoutHandler {

    /**
     * 处理加锁超时
     *
     * @param key 锁键
     * @param waitTime 等待时间
     * @param leaseTime 持有时间
     * @param lock 锁实例
     */
    void handle(String key,Long waitTime,Long leaseTime,Lock lock);

}
