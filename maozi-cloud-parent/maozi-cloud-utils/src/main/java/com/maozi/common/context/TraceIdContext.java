package com.maozi.common.context;

import com.maozi.common.ObjectUtil;
import org.slf4j.MDC;

/**
 * 链路追踪 ID 上下文
 * <p>
 * 存储当前线程的链路追踪 ID（traceId），设置非空值时同步写入 MDC 便于日志关联，
 * 清理时同步移除 MDC 中的链路追踪 ID。
 * </p>
 *
 * @author maozi
 * @date 2026/11/13
 */
public class TraceIdContext extends AbstractRequestContextHolder<String> {

    /** 链路追踪 ID 请求头名称 */
    public static final String KEY = "X-TraceId";

    /** 链路追踪 ID 未初始化时的占位值（32 个 0） */
    public static final String TRACE_ID_VALUE = "00000000000000000000000000000000";

    /** MDC 中存放链路追踪 ID 的键名 */
    public static final String MDC_TRACE_ID_KEY = "trace_id";

    /** 单例实例 */
    public static final TraceIdContext CONTEXT = new TraceIdContext();

    private TraceIdContext() {}

    /**
     * 设置当前线程的链路追踪 ID，非空时同时写入 MDC 便于日志关联
     *
     * @param traceId 链路追踪 ID，为空时不做任何处理
     */
    @Override
    public void set(String traceId) {
        if (ObjectUtil.isNotNullEmpty(traceId)) {
            super.set(traceId);
            MDC.put(MDC_TRACE_ID_KEY, traceId);
        }
    }

    /**
     * 清理当前线程的链路追踪 ID 及 MDC 中的链路追踪 ID
     */
    @Override
    public void remove() {
        super.remove();
        MDC.remove(MDC_TRACE_ID_KEY);
    }

}
