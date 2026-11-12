package com.maozi.lock.lock;

import cn.hutool.extra.spring.SpringUtil;
import com.maozi.common.enums.BaseEnum;
import com.maozi.lock.error.strategy.impl.LockTimeoutStrategy;
import com.maozi.lock.error.strategy.impl.UnLockTimeoutStrategy;
import com.maozi.lock.lock.impl.FairLock;
import com.maozi.lock.lock.impl.ReadLock;
import com.maozi.lock.lock.impl.ReentrantLock;
import com.maozi.lock.lock.impl.WriteLock;
import com.maozi.redis.utils.RedisUtil;
import lombok.Getter;

import java.util.function.Supplier;

/**
 * 锁类型枚举
 * <p>
 * 定义支持的分布式锁类型，包括可重入锁、公平锁、读锁和写锁。
 * 每种锁类型对应一个具体的锁实现类，通过枚举方法提供统一的加锁和解锁操作。
 * 支持编程式加锁（Supplier 回调）和声明式加锁（AOP 注解）两种使用方式。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
public enum LockType implements BaseEnum {

    /** 可重入锁：允许同一线程多次获取同一把锁 */
    REENTRANT(0, "可重入锁", ReentrantLock.class),

    /** 公平锁：按请求顺序依次获取锁，避免线程饥饿 */
    FAIR(1, "公平锁", FairLock.class),

    /** 读锁：允许多线程并发读取，阻塞写操作 */
    READ(2, "读锁", ReadLock.class),

    /** 写锁：独占锁，阻塞所有读写操作 */
    WRITE(3, "写锁", WriteLock.class),

    ;

    /**
     * 构造方法
     *
     * @param value 枚举值
     * @param desc 枚举描述
     * @param lockClass 锁实现类
     */
    LockType(Integer value,String desc, Class< ? extends Lock> lockClass) {

        this.value = value;

        this.desc = desc;

        this.lockClass = lockClass;

    }

    /** 枚举值 */
    @Getter
    private final Integer value;

    /** 枚举描述 */
    @Getter
    private final String desc;

    /**
     * 枚举的字符串表示，格式为 "值.描述"
     *
     * @return 格式化后的字符串
     */
    @Override
    public String toString() {
        return value + "." + desc;
    }

    /** 锁实现类 */
    private final Class< ? extends Lock > lockClass;

    /**
     * 从 Spring 容器中获取锁实现类实例
     *
     * @return 锁实现类实例
     */
    public Lock getLock(){
        return SpringUtil.getBean(lockClass);
    }

    /**
     * 编程式加锁并执行回调（默认超时策略：持续获取）
     *
     * @param key 锁键名
     * @param supplier 业务逻辑回调
     * @param <T> 返回值类型
     * @return 业务逻辑执行结果
     */
    public <T> T lock(String key, Supplier<T> supplier) {

        lock(key,60L,60L,LockTimeoutStrategy.KEEP_ACQUIRE);

        T t = supplier.get();

        unlock(key);

        return t;

    }

    /**
     * 编程式加锁并执行回调（自定义加锁超时策略）
     *
     * @param key 锁键名
     * @param strategy 加锁超时策略
     * @param supplier 业务逻辑回调
     * @param <T> 返回值类型
     * @return 业务逻辑执行结果
     */
    public <T> T lock(String key, LockTimeoutStrategy strategy, Supplier<T> supplier) {

        lock(key,60L,60L,strategy);

        T t = supplier.get();

        unlock(key);

        return t;

    }

    /**
     * 编程式加锁并执行回调（自定义加锁和解锁超时策略）
     *
     * @param key 锁键名
     * @param lockTimeoutStrategy 加锁超时策略
     * @param unLockTimeoutStrategy 解锁超时策略
     * @param supplier 业务逻辑回调
     * @param <T> 返回值类型
     * @return 业务逻辑执行结果
     */
    public <T> T lock(String key, LockTimeoutStrategy lockTimeoutStrategy,UnLockTimeoutStrategy unLockTimeoutStrategy, Supplier<T> supplier) {

        lock(key,60L,60L,lockTimeoutStrategy);

        T t = supplier.get();

        unlock(key,unLockTimeoutStrategy);

        return t;

    }

    /**
     * 编程式加锁并执行回调（自定义等待时间和持有时间）
     *
     * @param key 锁键名
     * @param waitTime 等待获取锁的最大时间（秒）
     * @param leaseTime 锁的持有时间（秒）
     * @param supplier 业务逻辑回调
     * @param <T> 返回值类型
     * @return 业务逻辑执行结果
     */
    public <T> T lock(String key, Long waitTime,Long leaseTime, Supplier<T> supplier) {

        lock(key,waitTime,leaseTime,LockTimeoutStrategy.KEEP_ACQUIRE);

        T t = supplier.get();

        unlock(key,UnLockTimeoutStrategy.NO_OPERATION);

        return t;

    }

    /**
     * 获取锁（使用默认超时参数，持续获取策略）
     *
     * @param key 锁键名
     */
    public void lock(String key)  {
        lock(key,60L,60L,LockTimeoutStrategy.KEEP_ACQUIRE);
    }

    /**
     * 获取锁（自定义加锁超时策略）
     *
     * @param key 锁键名
     * @param strategy 加锁超时策略
     */
    public void lock(String key, LockTimeoutStrategy strategy) {
        lock(key,60L,60L,strategy);
    }

    /**
     * 获取锁的核心方法
     * <p>
     * 通过 Spring 容器获取对应的锁实现类，在键名前添加服务名前缀以保证全局唯一性，
     * 若获取锁失败则交由超时策略处理。
     * </p>
     *
     * @param key 锁键名
     * @param waitTime 等待获取锁的最大时间（秒）
     * @param leaseTime 锁的持有时间（秒）
     * @param strategy 加锁超时策略
     */
    public void lock(String key,Long waitTime,Long leaseTime, LockTimeoutStrategy strategy) {

        // 从 Spring 容器中获取当前锁类型对应的锁实现类实例
        Lock lock = getLock();

        // 在键名前添加服务名前缀，确保不同服务的锁不会冲突
        key = RedisUtil.REDIS_KEY_PREFIX + "lock:" + key;

        // 尝试获取锁，如果获取失败则交由超时策略处理
        if(!lock.lock(key,waitTime,leaseTime)) {
            strategy.handle(key,waitTime,leaseTime, lock);
        }

    }

    /**
     * 释放锁（使用无操作解锁超时策略）
     *
     * @param key 锁键名
     */
    public void unlock(String key) {
        unlock(key,UnLockTimeoutStrategy.NO_OPERATION);
    }

    /**
     * 释放锁的核心方法
     * <p>
     * 在键名前添加服务名前缀以匹配加锁时的键名，
     * 若释放锁失败则交由解锁超时策略处理。
     * </p>
     *
     * @param key 锁键名
     * @param strategy 解锁超时策略
     */
    public void unlock(String key, UnLockTimeoutStrategy strategy) {

        // 从 Spring 容器中获取当前锁类型对应的锁实现类实例
        Lock lock = getLock();

        // 在键名前添加服务名前缀，与加锁时的键名保持一致
        key = RedisUtil.REDIS_KEY_PREFIX + "lock:" + key;

        // 尝试释放锁，如果释放失败则交由解锁超时策略处理
        if (!lock.unLock(key)) {
            strategy.handle();
        }

    }

    /**
     * 判断当前锁类型对应的锁是否处于锁定状态
     *
     * @param key 锁键名
     * @return 是否处于锁定状态
     */
    public boolean isLocked(String key){
        // 从 Spring 容器中获取当前锁类型对应的锁实现类实例
        Lock lock = getLock();

        // 在键名前添加服务名前缀，与加锁时的键名保持一致
        key = RedisUtil.REDIS_KEY_PREFIX + "lock:" + key;

        return lock.isLocked(key);

    }

}
