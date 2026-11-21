package com.maozi.gateway.config;

import com.maozi.common.CollectionUtil;
import com.maozi.common.JacksonUtil;
import com.maozi.common.LogUtil;
import com.maozi.common.context.TraceIdContext;
import com.maozi.common.result.error.code.SystemErrorCode;
import com.maozi.common.result.error.exception.BusinessResultException;
import com.maozi.common.result.success.SuccessResult;
import io.opentelemetry.api.trace.Span;
import jakarta.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.MDC;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * 服务端响应代理装饰器。
 * <p>
 * 继承 ServerHttpResponseDecorator，拦截响应体的写入过程，实现以下功能：
 * 1. 读取并缓存响应体内容；
 * 2. 判断响应体是否为自定义格式（包含 code 字段）；
 * 3. 记录请求耗时（RT）和响应数据到日志；
 * 4. 根据响应状态码和业务码决定日志级别（ERROR 或 INFO）。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Slf4j
public class ServerHttpResponseAgent extends ServerHttpResponseDecorator {

    /** 响应体中业务状态码的字段名 */
    private static final String RESULT_CODE_KEY = "code";

    /** 日志信息收集 Map，由 RequestLogFilter 传入并在此处补充响应数据 */
    private final Map<String, String> logs;

    /** 服务端请求对象，用于读取链路追踪 ID 请求头 */
    private final ServerHttpRequest request;

    /**
     * 构造响应代理装饰器。
     *
     * @param logs        日志信息收集 Map
     * @param response    原始服务端响应对象
     * @param request     服务端请求对象，用于读取链路追踪 ID 请求头
     */
    public ServerHttpResponseAgent(Map<String, String> logs, ServerHttpResponse response, 
                                   ServerHttpRequest request) {

        super(response);

        this.logs = logs;
        this.request = request;

    }

    /**
     * 拦截响应体写入。
     * <p>
     * 当响应体类型为 Flux 时，缓冲所有数据块并执行以下操作：
     * 1. 合并所有 DataBuffer 为字节数组；
     * 2. 解析 JSON 响应体，判断是否为自定义格式（含 code 字段）；
     * 3. 在 OpenTelemetry 未生成有效 traceId 时向 MDC 写入 traceId；
     * 4. 记录响应耗时和响应数据；
     * 5. 根据响应状态决定日志级别；
     * 6. 将合并后的字节重新包装为 DataBuffer 返回并写入下游。
     * </p>
     *
     * @param body 响应体数据流
     * @return 写入完成的 Void Mono
     */
    @Override
    public Mono<Void> writeWith(@Nonnull Publisher<? extends DataBuffer> body) {

        if (body instanceof Flux<? extends DataBuffer> fluxBody) {

            // 响应缓冲区工厂，用于将合并后的字节重新包装为 DataBuffer
            DataBufferFactory bufferFactory = super.getDelegate().bufferFactory();

            return super.writeWith(fluxBody.buffer()
                    .map(dataBuffers -> aggregateAndLog(dataBuffers, bufferFactory)));

        }

        // 非 Flux 类型直接透传
        return super.writeWith(body);

    }

    /**
     * 合并响应体数据块，记录响应日志并重新包装为 DataBuffer
     * <p>
     * 合并所有 DataBuffer 为字节数组后：解析 JSON 响应体判断是否为自定义格式、
     * 在 OpenTelemetry 未生成有效 traceId 时向 MDC 写入 traceId、
     * 根据响应状态和业务码记录对应级别的日志；
     * 最后包装结果字节返回，并关闭流与清理 MDC
     * </p>
     *
     * @param dataBuffers 响应体数据块列表
     * @param bufferFactory 响应缓冲区工厂
     * @return 合并后的响应数据缓冲区
     */
    private DataBuffer aggregateAndLog(List<? extends DataBuffer> dataBuffers, DataBufferFactory bufferFactory) {

        // 合并所有数据缓冲区的字节数据
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        dataBuffers.forEach(i -> {

            byte[] array = new byte[i.readableByteCount()];

            i.read(array);

            try {
                outputStream.write(array);
            } catch (IOException e) {
                // 内存字节流写入不会真正发生 IO 异常，防御性捕获并记录
                log.warn("write response buffer error", e);
            }

            // 释放 DataBuffer 资源，防止内存泄漏
            DataBufferUtils.release(i);

        });

        String responseBody = outputStream.toString();

        // 尝试将响应体解析为 Map，判断是否为自定义响应格式
        Map<?, ?> result = JacksonUtil.jsonToObject(responseBody, Map.class);

        // 当 OpenTelemetry 未生成有效 traceId 时，从请求头获取或生成 traceId 写入 MDC；
        // OTel 已有则由其 MDC 集成接管，不覆盖
        if (TraceIdContext.TRACE_ID_VALUE.equals(Span.current().getSpanContext().getTraceId())) {
            String traceId = request.getHeaders().getFirst(TraceIdContext.KEY);
            if (StringUtils.isEmpty(traceId)) {
                traceId = UUID.randomUUID().toString();
            }
            MDC.put(TraceIdContext.MDC_TRACE_ID_KEY, traceId);
        }

        // 根据响应状态和业务码决定日志级别
        if (Objects.requireNonNull(getDelegate().getStatusCode()).value() != HttpStatus.OK.value()) {
            // HTTP 状态码非 200，记录错误日志
            LogUtil.error(log, logs);
        } else if (CollectionUtil.isNotEmpty(result) && result.containsKey(RESULT_CODE_KEY)
                && !String.valueOf(SuccessResult.RESULT_SUCCESS_DEFAULT_CODE)
                    .equals(result.get(RESULT_CODE_KEY).toString())) {
            // 业务码非 200，记录错误日志
            LogUtil.error(log, logs);
        } else {
            // 正常响应，记录信息日志
            LogUtil.info(log, logs);
        }

        // 包装结果字节并返回，同时在 finally 中关闭流和清理 MDC
        DataBuffer buffer;
        try {
            buffer = bufferFactory.wrap(outputStream.toByteArray());
        } catch (Exception e) {
            // 包装失败时包装为业务异常向上抛出：若吞掉异常返回 null，
            // Reactor 的 map 会因 null 抛 NPE，掩盖真正的失败原因
            log.error("wrap response body error", e);
            throw new BusinessResultException(SystemErrorCode.DEFAULT_ERROR, e);
        } finally {
            try {
                outputStream.close();
            } catch (IOException e) {
                log.warn("close response buffer error", e);
            }
            MDC.clear();
        }
        return buffer;

    }

}
