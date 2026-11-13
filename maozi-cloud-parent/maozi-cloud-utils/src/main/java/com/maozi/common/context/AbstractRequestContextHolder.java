package com.maozi.common.context;

import com.alibaba.ttl.TransmittableThreadLocal;
import org.slf4j.MDC;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 请求链路上下文持有器父类
 * <p>
 * 内部持有 {@link TransmittableThreadLocal}，封装上下文值的获取、设置与清理，
 * 支持线程池和异步场景下的上下文传递。
 * 子类在构造时自动注册到持有器注册表，
 * 供 {@link #removeAll()} 循环清理所有继承了本类的上下文。
 * </p>
 *
 * @param <T> 上下文值类型
 * @author maozi
 * @date 2026/11/13
 */
public abstract class AbstractRequestContextHolder<T> {

    /** 所有继承了本类的上下文持有器（构造时自动注册） */
    private static final List<AbstractRequestContextHolder<?>> HOLDERS = new CopyOnWriteArrayList<>();

    /** 当前线程的上下文值 */
    private final TransmittableThreadLocal<T> context = new TransmittableThreadLocal<>();

    /**
     * 构造时自动注册到持有器注册表
     */
    protected AbstractRequestContextHolder() {
        HOLDERS.add(this);
    }

    /**
     * 获取当前线程的上下文值
     *
     * @return 上下文值，未设置时返回 null
     */
    public T get() {
        return context.get();
    }

    /**
     * 设置当前线程的上下文值
     *
     * @param value 上下文值
     */
    public void set(T value) {
        context.set(value);
    }

    /**
     * 清理当前线程的上下文值
     */
    public void remove() {
        context.remove();
    }

    /**
     * 循环清理所有继承了本类的上下文持有器中当前线程的上下文值
     */
    public static void removeAll() {
        HOLDERS.forEach(AbstractRequestContextHolder::remove);
    }

    /**
     * 清理当前线程的链路上下文（含所有上下文持有器及 MDC）
     */
    public static void clearContext() {
        AbstractRequestContextHolder.removeAll();
        MDC.clear();
    }

}
