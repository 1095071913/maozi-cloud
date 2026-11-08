package com.maozi.lock.annotation;

import com.maozi.lock.error.strategy.impl.LockTimeoutStrategy;
import com.maozi.lock.error.strategy.impl.UnLockTimeoutStrategy;
import com.maozi.lock.lock.LockType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 分布式锁注解
 * <p>
 * 标注在方法上，声明方法在获取分布式锁后方可执行；
 * 支持可重入锁、公平锁、读写锁，锁键可通过 SpEL 表达式或 {@link LockKey} 指定。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
@Target(value = {ElementType.METHOD})
@Retention(value = RetentionPolicy.RUNTIME)
public @interface Lock {

    /** 锁名称 */
    String name() default "";

    /** 锁类型，默认可重入锁 */
    LockType type() default LockType.REENTRANT;

    /** 尝试加锁的最长等待时间（秒），默认 Long.MIN_VALUE 表示回退到全局配置 */
    long waitTime() default Long.MIN_VALUE;

    /** 加锁成功后自动释放锁的时长（秒），默认 Long.MIN_VALUE 表示回退到全局配置 */
    long leaseTime() default Long.MIN_VALUE;

     /** 自定义业务键（支持 SpEL 表达式） */
     String [] keys() default {};

     /** 加锁超时的处理策略，默认持续获取（指数退避重试） */
     LockTimeoutStrategy lockTimeoutStrategy() default LockTimeoutStrategy.KEEP_ACQUIRE;

     /** 释放锁时已超时的处理策略，默认不做处理 */
     UnLockTimeoutStrategy releaseTimeoutStrategy() default UnLockTimeoutStrategy.NO_OPERATION;

}