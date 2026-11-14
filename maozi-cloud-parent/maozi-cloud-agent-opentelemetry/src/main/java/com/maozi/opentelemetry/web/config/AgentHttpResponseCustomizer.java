package com.maozi.opentelemetry.web.config;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.bootstrap.http.HttpServerResponseCustomizer;
import io.opentelemetry.javaagent.bootstrap.http.HttpServerResponseMutator;
import io.opentelemetry.javaagent.shaded.io.opentelemetry.api.trace.Span;
import io.opentelemetry.javaagent.shaded.io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.javaagent.shaded.io.opentelemetry.context.Context;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * HTTP 响应追踪 ID 注入定制器
 * <p>
 * 通过 OpenTelemetry SPI（{@link AutoService}）自动注册，在网关响应中注入
 * {@code X-TraceId} 响应头，便于客户端通过响应头获取链路追踪 ID。
 * 仅对 Netty 的 {@code DefaultHttpResponse} / {@code DefaultFullHttpResponse}
 * 类型生效（即网关转发场景），Service 层的 Undertow 响应不在此处理范围。
 * </p>
 *
 * @author pengjinlong
 * @date 2026/07/04
 */
@AutoService(HttpServerResponseCustomizer.class)
public class AgentHttpResponseCustomizer implements HttpServerResponseCustomizer {

    /** 追踪 ID 响应头名称 */
    private static final String TRACE_ID_HEADER_KEY = "X-TraceId";

    /** 需要注入追踪 ID 的响应类型（网关转发的 Netty 响应） */
    private final List<String> ADD_TRACEID_CLASS = Collections.unmodifiableList(
        Arrays.asList("io.netty.handler.codec.http.DefaultHttpResponse","io.netty.handler.codec.http.DefaultFullHttpResponse")
    );

    /**
     * 在响应中注入 {@code X-TraceId} 响应头
     * <p>
     * 仅当响应类型为网关转发的 Netty 响应时注入，从当前 Span 上下文提取追踪 ID。
     * </p>
     *
     * @param context 当前上下文
     * @param response 原始响应对象
     * @param responseMutator 响应修改器，用于追加响应头
     * @param <RESPONSE> 响应对象类型
     */
    @Override
    public <RESPONSE> void customize(Context context, RESPONSE response, HttpServerResponseMutator<RESPONSE> responseMutator) {

        // 从当前上下文提取 Span 上下文，用于获取追踪 ID
        SpanContext spanContext = Span.fromContext(context).getSpanContext();

        // 网关未找到目标服务时的转发响应 = io.netty.handler.codec.http.DefaultFullHttpResponse
        // 网关找到目标服务时的转发响应 = io.netty.handler.codec.http.DefaultHttpResponse
        // 服务层的响应对象 = io.undertow.server.HttpServerExchange（不注入）
        if(ADD_TRACEID_CLASS.contains(response.getClass().getName())){
            responseMutator.appendHeader(response, TRACE_ID_HEADER_KEY, spanContext.getTraceId());
        }

    }

}