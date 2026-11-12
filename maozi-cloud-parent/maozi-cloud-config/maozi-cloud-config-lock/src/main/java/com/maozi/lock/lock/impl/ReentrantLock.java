package com.maozi.lock.lock.impl;

import com.maozi.lock.lock.Lock;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 可重入锁实现
 * <p>
 * 基于 Redisson 的可重入锁（Reentrant Lock），支持同一线程多次获取同一把锁。
 * 加锁使用 {@code tryLock(waitTime, leaseTime, TimeUnit.SECONDS)}：在 {@code waitTime} 内阻塞等待获取锁，
 * 超时则返回 false；获取成功后锁会在 {@code leaseTime} 到期自动释放。
 * 解锁使用 {@code forceUnlockAsync} 强制异步释放。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Component
public class ReentrantLock implements Lock {

    /** Redisson 客户端 */
    @Resource
    private RedissonClient redissonClient;

    /**
     * 尝试获取可重入锁
     *
     * @param key 锁的键名
     * @param waitTime 等待获取锁的最大时间（秒）
     * @param leaseTime 锁的持有时间（秒），超时后自动释放
     * @return 是否成功获取锁
     */
    @Override
    @SneakyThrows
    public boolean lock(String key,Long waitTime,Long leaseTime) {
        return redissonClient.getLock(key).tryLock(waitTime,leaseTime,TimeUnit.SECONDS);
    }

    /**
     * 释放可重入锁
     * <p>
     * 仅当锁由当前线程持有时才执行释放操作，避免误释放其他线程的锁。
     * </p>
     *
     * @param key 锁的键名
     * @return 是否成功释放锁
     */
    @Override
    @SneakyThrows
    public boolean unLock(String key) {

        RLock lock = redissonClient.getLock(key);

        // 判断锁是否由当前线程持有，如果是则异步强制释放锁并等待结果，否则返回 false
        return lock.isHeldByCurrentThread() && lock.forceUnlockAsync().get();

    }

    /**
     * 判断可重入锁是否处于锁定状态
     *
     * @param key 锁的键名
     * @return 是否处于锁定状态
     */
    @Override
    public boolean isLocked(String key) {
        return redissonClient.getLock(key).isLocked();
    }

}
