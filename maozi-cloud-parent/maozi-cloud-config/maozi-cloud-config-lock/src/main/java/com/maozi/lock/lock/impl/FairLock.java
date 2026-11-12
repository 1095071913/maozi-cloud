package com.maozi.lock.lock.impl;

import com.maozi.lock.lock.Lock;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 公平锁实现
 * <p>
 * 基于 Redisson 的公平锁（Fair Lock），按照请求的先后顺序依次获取锁，
 * 避免线程饥饿问题。适用于对锁获取顺序有严格要求的业务场景。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Component
public class FairLock implements Lock {

    /** Redisson 客户端 */
    @Resource
    private RedissonClient redissonClient;

    /**
     * 尝试获取公平锁
     *
     * @param key 锁的键名
     * @param waitTime 等待获取锁的最大时间（秒）
     * @param leaseTime 锁的持有时间（秒），超时后自动释放
     * @return 是否成功获取锁
     */
    @Override
    @SneakyThrows
    public boolean lock(String key,Long waitTime,Long leaseTime) {
        return redissonClient.getFairLock(key).tryLock(waitTime,leaseTime,TimeUnit.SECONDS);
    }

    /**
     * 释放公平锁
     * <p>
     * 仅当锁由当前线程持有时才执行释放操作。
     * </p>
     *
     * @param key 锁的键名
     * @return 是否成功释放锁
     */
    @Override
    @SneakyThrows
    public boolean unLock(String key) {

        RLock lock = redissonClient.getFairLock(key);

        // 判断公平锁是否由当前线程持有，如果是则异步强制释放锁并等待结果，否则返回 false
        return lock.isHeldByCurrentThread() && lock.forceUnlockAsync().get();

    }

    /**
     * 判断公平锁是否处于锁定状态
     *
     * @param key 锁的键名
     * @return 是否处于锁定状态
     */
    @Override
    public boolean isLocked(String key) {
        return redissonClient.getFairLock(key).isLocked();
    }

}
