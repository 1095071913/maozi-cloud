package com.maozi.common.context;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 透传异常类容器
 * <p>
 * 存放请求入口日志切面需要原样上抛（不封装为统一错误结果）的异常类型。
 * 各组件模块在启动时向容器注册自身领域需交由对应框架处理的异常类型
 * （如 maozi-cloud-config-oauth 注册 Spring Security 的权限拒绝异常），
 * 请求入口日志切面捕获异常后通过 {@link #contains(Throwable)} 判断是否透传。
 * </p>
 * <p>
 * 本模块不依赖具体异常类型所在的框架，仅承载异常类型本身，
 * 以此实现日志模块与异常来源模块的解耦。
 * </p>
 *
 * @author maozi
 * @date 2026/11/22
 */
public final class PassThroughExceptionContainer {

    /**
     * 透传异常类型集合
     * （仅类内部可写，外部通过 {@link #add(Class)} 注册；启动线程写入后供任意请求线程并发读，
     * 使用并发集合保证跨线程安全发布）
     */
    private static final Set<Class<? extends Throwable>> EXCEPTION_CLASSES = ConcurrentHashMap.newKeySet();

    /**
     * 私有构造器，防止外部实例化
     */
    private PassThroughExceptionContainer() {
    }

    /**
     * 注册需要原样上抛的异常类型
     *
     * @param exceptionClass 异常类型
     */
    public static void add(Class<? extends Throwable> exceptionClass) {
        EXCEPTION_CLASSES.add(exceptionClass);
    }

    /**
     * 判断异常是否为透传异常
     * <p>
     * 已注册类型的子类同样视为透传异常
     * （如 Spring Security 方法级鉴权抛出的 AuthorizationDeniedException
     * 为 AccessDeniedException 的子类）。
     * </p>
     *
     * @param e 待判断的异常
     * @return 异常类型或其父类型已注册时返回 true
     */
    public static boolean contains(Throwable e) {
        for (Class<? extends Throwable> exceptionClass : EXCEPTION_CLASSES) {
            if (exceptionClass.isInstance(e)) {
                return true;
            }
        }
        return false;
    }

}
