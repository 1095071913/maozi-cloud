/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.maozi.gateway.config;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.maozi.common.LogUtil;
import com.maozi.common.ObjectUtil;
import com.maozi.common.ResultUtil;
import com.maozi.common.constant.LogTag;
import com.maozi.common.context.ApplicationLinkContext;
import com.maozi.common.enums.LogCommonType;
import com.maozi.common.result.error.ErrorResult;
import com.maozi.common.result.error.code.SystemErrorCode;
import com.maozi.gateway.utils.WebUtil;
import io.opentelemetry.api.trace.Span;
import jakarta.annotation.Nonnull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jboss.logging.MDC;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.netty.channel.AbortedException;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * 网关全局异常处理器。
 * <p>
 * 实现 ErrorWebExceptionHandler 接口，统一处理网关层所有异常：
 * - 服务未找到异常（NotFoundException）：返回服务不存在的错误响应；
 * - Sentinel 限流异常（BlockException）：返回限流错误响应；
 * - 其他未知异常：返回系统内部错误响应。
 * 同时记录详细的错误日志，包含请求 IP、URL、方法、异常信息等。
 * </p>
 *
 * @author maozi
 */
@Slf4j
@Component
public class GatewayExceptionHandler implements ErrorWebExceptionHandler {

	/** HTTP 消息读取器列表，用于解析请求体 */
	private List<HttpMessageReader<?>> messageReaders = Collections.emptyList();

	/** HTTP 消息写入器列表，用于序列化响应体 */
	private List<HttpMessageWriter<?>> messageWriters = Collections.emptyList();

	/** 视图解析器列表，用于视图渲染
     * -- SETTER --
     *  设置视图解析器列表。
     *
     * @param viewResolvers 视图解析器列表
     */
	@Setter
    private List<ViewResolver> viewResolvers = Collections.emptyList();

	/** 线程本地变量，暂存异常处理结果，用于后续响应渲染 */
	private final ThreadLocal<ErrorResult<?>> exceptionHandlerResult = new ThreadLocal<>();

	/**
	 * 设置 HTTP 消息读取器。
	 *
	 * @param messageReaders 消息读取器列表
	 */
	public void setMessageReaders(List<HttpMessageReader<?>> messageReaders) {
		Assert.notNull(messageReaders, "'messageReaders' must not be null");
		this.messageReaders = messageReaders;
	}

    /**
	 * 设置 HTTP 消息写入器。
	 *
	 * @param messageWriters 消息写入器列表
	 */
	public void setMessageWriters(List<HttpMessageWriter<?>> messageWriters) {
		Assert.notNull(messageWriters, "'messageWriters' must not be null");
		this.messageWriters = messageWriters;
	}

	/**
	 * 渲染错误响应。
	 * <p>
	 * 从 ThreadLocal 中获取异常处理结果，构造 JSON 格式的错误响应返回给客户端。
	 * </p>
	 *
	 * @param request 服务端请求对象
	 * @return 包含错误信息的 ServerResponse
	 */
	protected Mono<ServerResponse> renderErrorResponse(ServerRequest request) {
		ErrorResult<?> result = exceptionHandlerResult.get();
		return ServerResponse.status(result.getHttpCode()).contentType(MediaType.APPLICATION_JSON)
				.body(BodyInserters.fromValue(result));
	}

	/**
	 * 将 ServerResponse 写入到 ServerWebExchange 中。
	 *
	 * @param exchange 服务端 Web 交换上下文
	 * @param response 要写入的服务端响应
	 * @return 写入完成的 Void Mono
	 */
	private Mono<? extends Void> write(ServerWebExchange exchange, ServerResponse response) {
		exchange.getResponse().getHeaders().setContentType(response.headers().getContentType());
		return response.writeTo(exchange, new ResponseContext());
	}

	/**
	 * ServerResponse 上下文实现类。
	 * <p>
	 * 提供消息写入器和视图解析器给 ServerResponse 使用。
	 * </p>
	 *
	 * @author maozi
	 */
	private class ResponseContext implements ServerResponse.Context {

		/**
		 * 返回外部类的 HTTP 消息写入器列表
		 *
		 * @return HTTP 消息写入器列表
		 */
		@Override
		public List<HttpMessageWriter<?>> messageWriters() {
			return GatewayExceptionHandler.this.messageWriters;
		}

		/**
		 * 返回外部类的视图解析器列表
		 *
		 * @return 视图解析器列表
		 */
		@Override
		public List<ViewResolver> viewResolvers() {
			return GatewayExceptionHandler.this.viewResolvers;
		}
	}

	/**
	 * 处理网关异常的核心方法。
	 * <p>
	 * 根据异常类型返回不同的错误响应：
	 * - 客户端断连异常（AbortedException）：不记录错误日志，直接透传异常；
	 * - NotFoundException：服务不存在，返回对应的错误码和 HTTP 状态码；
	 * - BlockException（Sentinel 限流）：返回限流错误码和 HTTP 状态码；
	 * - 其他异常：返回系统内部错误码和 HTTP 状态码。
	 * 同时在 OpenTelemetry 未生成有效 traceId 时向 MDC 写入 traceId，
	 * 并记录完整的请求信息和错误详情。
	 * </p>
	 *
	 * @param exchange 服务端 Web 交换上下文
	 * @param e        捕获到的异常
	 * @return 异常处理完成的 Void Mono
	 */
	@Override
	public Mono<Void> handle(ServerWebExchange exchange, @Nonnull Throwable e) {

		ServerHttpRequest request = exchange.getRequest();

		// 客户端在响应发送过程中断开连接（AbortedException，含 Connection closed/reset、ClosedChannelException 包装）属于正常业务现象，
		// 连接已关闭无法写回错误响应，不记录错误日志直接透传（透传后由 ReactorHooksConfiguration 的 onErrorDropped 钩子静默处理）
		if (e instanceof AbortedException) {
			return Mono.error(e);
		}

		// 当 OpenTelemetry 未生成有效 traceId 时，从请求头获取或生成 traceId 写入 MDC；OTel 已有则由其 MDC 集成接管，不覆盖
		if (ApplicationLinkContext.TRACE_ID_VALUE.equals(Span.current().getSpanContext().getTraceId())) {
			String traceId = request.getHeaders().getFirst(ApplicationLinkContext.TRACE_ID_KEY);
			if (ObjectUtil.isNullEmpty(traceId)) {
				traceId = UUID.randomUUID().toString();
			}
			MDC.put(ApplicationLinkContext.MDC_TRACE_ID_KEY, traceId);
		}

		// 构建错误日志信息
		Map<String,String> logs = new LinkedHashMap<>();

		logs.put(LogTag.TYPE, LogCommonType.GATEWAY.getDesc());
		logs.put(LogTag.IP, WebUtil.getRequestHost(request));
		logs.put(LogTag.URL, request.getURI().toString());
		logs.put(LogTag.METHOD, Objects.requireNonNull(request.getMethod()).toString());
        logs.put(LogTag.ERROR_DESC, e.getLocalizedMessage());

		// 记录异常堆栈日志
		LogUtil.error(log,e);

		// 根据异常类型构建不同的错误响应
		ErrorResult<?> result;

		if (e instanceof NotFoundException) {
			// 服务未找到异常
			result = ResultUtil.error(SystemErrorCode.SERVICE_NOT_EXIST_ERROR).setHttpCode(SystemErrorCode.SYSTEM_ERROR_DEFAULT_CODE);
		}else if(BlockException.isBlockException(e)){
			// Sentinel 限流异常
			result = ResultUtil.error(SystemErrorCode.CURRENT_LIMITING_ERROR).setHttpCode(SystemErrorCode.CURRENT_LIMITING_ERROR_DEFAULT_CODE);
		}else {
			// 其他未知系统异常
			result = ResultUtil.error(SystemErrorCode.SYSTEM_ERROR).setHttpCode(SystemErrorCode.SYSTEM_ERROR_DEFAULT_CODE);
		}

		logs.put(LogTag.DATA, result.toString());

		// 记录错误响应日志
		LogUtil.error(log,logs);

		// 清除 MDC 上下文，防止内存泄漏
		MDC.clear();

		// 如果响应已提交（已发送给客户端），则无法修改，直接返回异常
		if (exchange.getResponse().isCommitted()) {
			return Mono.error(e);
		}

		// 将错误结果暂存到 ThreadLocal，供 renderErrorResponse 使用
		exceptionHandlerResult.set(result);

		// 创建新的 ServerRequest 并路由到错误响应处理器
		ServerRequest newRequest = ServerRequest.create(exchange, this.messageReaders);
		return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse).route(newRequest)
				.switchIfEmpty(Mono.error(e)).flatMap((handler) -> handler.handle(newRequest))
				.flatMap((response) -> write(exchange, response));
	}

}
