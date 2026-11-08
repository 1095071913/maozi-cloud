package com.maozi.lock.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 分布式锁业务键注解
 * <p>
 * 标注在方法参数上，声明该参数作为分布式锁的业务键。
 * 支持通过 SpEL 表达式提取参数中的特定字段作为锁键。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = {ElementType.PARAMETER, ElementType.TYPE})
public @interface LockKey {

    /** SpEL 表达式，用于从参数对象中提取锁键值，为空时直接使用参数的 toString 值 */
    String value() default "";

}
