package com.maozi.gateway.filter;

import com.maozi.common.ObjectUtil;
import com.maozi.common.context.ApplicationLinkContext;
import io.opentelemetry.api.trace.Span;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * 链路追踪 ID 全局过滤器。
 * <p>
 * 在网关入口处生成或继承请求头 {@link ApplicationLinkContext#TRACE_ID_KEY} 中的链路追踪 ID，
 * 并将其注入到下游路由请求头中，实现网关到微服务的全链路追踪。
 * </p>
 * <p>
 * 当 OpenTelemetry 当前未生成有效 Span（traceId 为占位值 {@link ApplicationLinkContext#TRACE_ID_VALUE}）时，
 * 采用自定义追踪 ID 策略：优先复用请求头中已有的 traceId，缺失时生成 UUID。
 * 将 traceId 写入 MDC 以关联网关日志，请求结束后清理 MDC 防止线程复用导致的数据泄漏。
 * </p>
 *
 * @author maozi
 */
@Component
public class TraceIdFilter implements GlobalFilter, Ordered {

	/**
	 * 过滤器核心逻辑。
	 * <p>
	 * 1. 判断 OpenTelemetry 是否已存在有效链路追踪；
	 * 2. 若不存在，则复用或生成 traceId，并写入请求头供下游路由服务延续链路；
	 * 3. 将 traceId 写入 MDC 与线程上下文，使网关日志可关联链路；
	 * 4. 请求结束后（无论成功或失败）清理 MDC 与链路上下文。
	 * </p>
	 *
	 * @param exchange 服务端 Web 交换上下文
	 * @param chain    网关过滤器链
	 * @return 过滤器链执行结果
	 */
	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

		// 当 OpenTelemetry 未生成有效 traceId 时，使用自定义链路追踪 ID
		if (ApplicationLinkContext.TRACE_ID_VALUE.equals(Span.current().getSpanContext().getTraceId())) {

			// 优先复用请求头中携带的 traceId，缺失时生成 UUID
			String traceId = exchange.getRequest().getHeaders().getFirst(ApplicationLinkContext.TRACE_ID_KEY);
			if (ObjectUtil.isNullEmpty(traceId)) {
				traceId = UUID.randomUUID().toString();
			}

			// 将 traceId 写入请求头，供下游路由服务延续链路
			ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
				.header(ApplicationLinkContext.TRACE_ID_KEY, traceId)
				.build();

			// 写入 MDC 与线程上下文，使当前线程的网关日志可关联链路追踪 ID
			ApplicationLinkContext.setTraceId(traceId);

			return chain.filter(exchange.mutate().request(mutatedRequest).build())
				// 请求结束后清理 MDC 与链路上下文，防止线程复用导致的数据泄漏
				.doFinally(signal -> ApplicationLinkContext.clearContext());

		}

		// OpenTelemetry 已存在有效链路追踪，由其 MDC 集成接管 traceId，直接放行
		return chain.filter(exchange);

	}

	/**
	 * 获取过滤器执行顺序。
	 * <p>
	 * 设置为 HIGHEST_PRECEDENCE，确保在请求进入网关的第一时间建立链路追踪上下文，
	 * 使后续过滤器（如请求日志记录）的日志可关联到 traceId。
	 * </p>
	 *
	 * @return 过滤器顺序值
	 */
	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}

}
