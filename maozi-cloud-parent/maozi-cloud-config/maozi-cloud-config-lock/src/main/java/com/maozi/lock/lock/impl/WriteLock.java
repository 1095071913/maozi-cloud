package com.maozi.lock.lock.impl;

import com.maozi.lock.lock.Lock;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 写锁实现
 * <p>
 * 基于 Redisson 的读写锁中的写锁（Write Lock），同一时刻仅允许一个线程持有写锁，
 * 且在有写锁持有时，所有读锁和写锁请求都将被阻塞。适用于数据修改等需要独占访问的场景。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Component
public class WriteLock implements Lock {

    /** Redisson 客户端 */
    @Resource
    private RedissonClient redissonClient;

    /**
     * 尝试获取写锁
     *
     * @param key 锁的键名
     * @param waitTime 等待获取锁的最大时间（秒）
     * @param leaseTime 锁的持有时间（秒），超时后自动释放
     * @return 是否成功获取锁
     */
    @Override
    @SneakyThrows
    public boolean lock(String key,Long waitTime,Long leaseTime) {
        return redissonClient.getReadWriteLock(key).writeLock().tryLock(waitTime,leaseTime,TimeUnit.SECONDS);
    }

    /**
     * 释放写锁
     * <p>
     * 仅当写锁由当前线程持有时才执行释放操作。
     * </p>
     *
     * @param key 锁的键名
     * @return 是否成功释放锁
     */
    @Override
    @SneakyThrows
    public boolean unLock(String key) {

        RReadWriteLock lock = redissonClient.getReadWriteLock(key);

        // 判断写锁是否由当前线程持有，如果是则异步强制释放写锁并等待结果，否则返回 false
        return lock.writeLock().isHeldByCurrentThread() && lock.writeLock().forceUnlockAsync().get();

    }

    /**
     * 判断写锁是否处于锁定状态
     *
     * @param key 锁的键名
     * @return 是否处于锁定状态
     */
    @Override
    public boolean isLocked(String key) {
        return redissonClient.getReadWriteLock(key).writeLock().isLocked();
    }

}
