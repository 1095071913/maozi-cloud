package com.maozi.lock.lock;

/**
 * 分布式锁接口
 * <p>
 * 定义分布式锁的加锁和解锁操作，由不同的锁实现类
 * （可重入锁、公平锁、读写锁等）分别实现具体的锁策略。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
public interface Lock {

    /**
     * 尝试获取分布式锁
     *
     * @param key 锁的键名
     * @param waitTime 等待获取锁的最大时间（秒）
     * @param leaseTime 锁的持有时间（秒），超时后自动释放
     * @return 是否成功获取锁
     */
    boolean lock(String key,Long waitTime,Long leaseTime);

    /**
     * 释放分布式锁
     *
     * @param key 锁的键名
     * @return 是否成功释放锁
     */
    boolean unLock(String key);

    /**
     * 判断锁是否处于锁定状态
     *
     * @param key 锁的键名
     * @return 是否处于锁定状态
     */
    boolean isLocked(String key);

}
