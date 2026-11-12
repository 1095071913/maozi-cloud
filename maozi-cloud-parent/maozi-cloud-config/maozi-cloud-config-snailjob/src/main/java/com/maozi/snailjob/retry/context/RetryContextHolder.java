package com.maozi.snailjob.retry.context;

import com.alibaba.ttl.TransmittableThreadLocal;

/**
 * 自定义重试流量上下文
 * <p>
 * 用于判断当前流量是否为 SnailJob 服务端调度回来的远程重试请求。
 * 统一重试处理器 {@code RetryExecutorTask} 回调目标方法前标记重试流量，
 * {@code RetryTaskReportAop} 据此区分正常流量与重试流量：
 * 重试流量的执行结果或异常直接放行，不再触发本地重试与上报。
 * 使用 {@link TransmittableThreadLocal} 支持线程池场景下的上下文传递。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
public class RetryContextHolder {

    /** 是否是远程重试流量的线程变量（TTL） */
    private static final ThreadLocal<Boolean> THREAD_LOCAL_RETRY = new TransmittableThreadLocal<>();

    /**
     * 获取当前线程是否是重试请求
     *
     * @return 是否是重试请求，未标记时返回 {@code null}
     */
    public static Boolean getRetry() {
        return THREAD_LOCAL_RETRY.get();
    }

    /**
     * 标记当前线程是否是重试请求
     *
     * @param retry 是否是重试请求
     */
    public static void setRetry(Boolean retry) {
        THREAD_LOCAL_RETRY.set(retry);
    }

    /**
     * 清理当前线程的重试请求标记
     */
    public static void clear() {
        THREAD_LOCAL_RETRY.remove();
    }

}
