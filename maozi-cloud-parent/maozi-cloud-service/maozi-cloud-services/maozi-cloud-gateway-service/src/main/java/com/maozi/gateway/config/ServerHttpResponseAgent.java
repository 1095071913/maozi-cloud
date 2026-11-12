package com.maozi.gateway.config;

import com.maozi.common.JacksonUtil;
import com.maozi.common.LogUtil;
import com.maozi.common.ObjectUtil;
import com.maozi.common.constant.LogTag;
import com.maozi.common.context.ApplicationLinkContext;
import com.maozi.common.result.success.SuccessResult;
import io.opentelemetry.api.trace.Span;
import jakarta.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
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

    /** 请求开始时间戳（毫秒），用于计算响应耗时 */
    private final Long requestTime;

    /** 日志信息收集 Map，由 RequestLogFilter 传入并在此处补充响应数据 */
    private final Map<String, String> logs;

    /** 服务端请求对象，用于读取链路追踪 ID 请求头 */
    private final ServerHttpRequest request;

    /**
     * 构造响应代理装饰器。
     *
     * @param requestTime 请求开始时间戳（毫秒）
     * @param logs        日志信息收集 Map
     * @param response    原始服务端响应对象
     * @param attributes  Exchange 上下文属性 Map（当前实现未使用）
     * @param request     服务端请求对象，用于读取链路追踪 ID 请求头
     */
    public ServerHttpResponseAgent(Long requestTime, Map<String, String> logs, ServerHttpResponse response,
        Map<String, Object> attributes, ServerHttpRequest request) {

        super(response);

        this.requestTime = requestTime;

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

            DataBufferFactory bufferFactory = getDelegate().bufferFactory();

            return super.writeWith(fluxBody.buffer().map(dataBuffers -> {

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

                // 尝试将响应体解析为 Map，判断是否为自定义响应格式
                Map<?, ?> result = JacksonUtil.jsonToObject(outputStream.toString(), Map.class);

                // 当 OpenTelemetry 未生成有效 traceId 时，从请求头获取或生成 traceId 写入 MDC；
                // OTel 已有则由其 MDC 集成接管，不覆盖
                if (ApplicationLinkContext.TRACE_ID_VALUE.equals(Span.current().getSpanContext().getTraceId())) {
                    String traceId = request.getHeaders().getFirst(ApplicationLinkContext.TRACE_ID_KEY);
                    if (ObjectUtil.isNullEmpty(traceId)) {
                        traceId = UUID.randomUUID().toString();
                    }
                    MDC.put(ApplicationLinkContext.MDC_TRACE_ID_KEY, traceId);
                }

                // 记录响应耗时（RT）
                logs.put(LogTag.RT, System.currentTimeMillis() - requestTime + " ms");
                // 记录响应数据
                logs.put(LogTag.DATA, outputStream.toString());

                // 根据响应状态和业务码决定日志级别
                if (Objects.requireNonNull(getDelegate().getStatusCode()).value() != HttpStatus.OK.value()) {
                    // HTTP 状态码非 200，记录错误日志
                    LogUtil.error(log, logs);
                } else if (ObjectUtil.isNotNullEmpty(result) && result.containsKey(RESULT_CODE_KEY)
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
                    log.error("wrap response body error", e);
                    buffer = null;
                } finally {
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        log.warn("close response buffer error", e);
                    }
                    MDC.clear();
                }
                return buffer;
            }));

        }

        // 非 Flux 类型直接透传
        return super.writeWith(body);

    }

}
