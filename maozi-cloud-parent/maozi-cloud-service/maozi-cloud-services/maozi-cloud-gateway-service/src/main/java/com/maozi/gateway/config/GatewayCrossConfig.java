package com.maozi.gateway.config;

import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.gateway.discovery.DiscoveryClientRouteDefinitionLocator;
import org.springframework.cloud.gateway.discovery.DiscoveryLocatorProperties;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.codec.support.DefaultServerCodecConfigurer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.reactive.CorsUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;


/**
 * 网关跨域与路由发现配置类。
 * <p>
 * 主要功能：
 * 1. 基于服务发现（Nacos）自动创建路由规则；
 * 2. 配置 HTTP 编解码器；
 * 3. 提供全局 CORS 跨域过滤器，允许前端跨域访问网关。
 * </p>
 *
 * @author maozi
 */
@Component
public class GatewayCrossConfig {

	/** 允许所有响应头暴露给前端 */
	private static final String ALL = "*";

	/** CORS 预检请求缓存时间（秒）；注意值带 long 后缀 L，浏览器会收到字面 "3600L"，如需修正请改为 "3600" */
	private static final String MAX_AGE = "3600L";

	/**
	 * 基于服务发现的路由定义定位器。
	 * <p>
	 * 通过 ReactiveDiscoveryClient 自动从注册中心（如 Nacos）获取服务实例，
	 * 并根据 DiscoveryLocatorProperties 的配置自动生成路由规则。
	 * </p>
	 *
	 * @param discoveryClient 响应式服务发现客户端
	 * @param properties      服务发现路由定位属性配置
	 * @return 路由定义定位器
	 */
	@Bean
	public RouteDefinitionLocator discoveryClientRouteDefinitionLocator(ReactiveDiscoveryClient discoveryClient,DiscoveryLocatorProperties properties) {
		return new DiscoveryClientRouteDefinitionLocator(discoveryClient, properties);
	}

	/**
	 * HTTP 消息编解码器配置。
	 * <p>
	 * 提供请求/响应体的编解码能力，用于 JSON、表单等数据的序列化与反序列化。
	 * </p>
	 *
	 * @return 服务端编解码器配置
	 */
	@Bean
	public ServerCodecConfigurer serverCodecConfigurer() {
		return new DefaultServerCodecConfigurer();
	}

	/**
	 * CORS 跨域过滤器。
	 * <p>
	 * 拦截所有请求，对于跨域请求（由 Origin 头判断）：
	 * - 设置 Access-Control-Allow-Origin 为请求来源；
	 * - 允许携带凭证（Cookies）；
	 * - 暴露所有响应头给前端；
	 * - 设置预检请求缓存时间（Access-Control-Max-Age，取 MAX_AGE 常量值）；
	 * - 对 OPTIONS 预检请求直接返回 200。
	 * </p>
	 *
	 * @return CORS 跨域 Web 过滤器
	 */
	@Bean
	public WebFilter corsFilter() {
		return (ServerWebExchange ctx, WebFilterChain chain) -> {
			ServerHttpRequest request = ctx.getRequest();

			// 非跨域请求直接放行
			if (!CorsUtils.isCorsRequest(request)) {
				return chain.filter(ctx);
			}

			HttpHeaders requestHeaders = request.getHeaders();
			ServerHttpResponse response = ctx.getResponse();
			HttpMethod requestMethod = requestHeaders.getAccessControlRequestMethod();
			HttpHeaders headers = response.getHeaders();

			// 允许请求来源域
			headers.add(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, requestHeaders.getOrigin());
			// 允许的请求头
			headers.addAll(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, requestHeaders.getAccessControlRequestHeaders());
			// 允许的 HTTP 方法
			if (requestMethod != null) {
				headers.add(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, requestMethod.name());
			}
			// 允许携带凭证
			headers.add(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
			// 暴露给前端的响应头
			headers.add(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, ALL);
			// 预检请求缓存时间
			headers.add(HttpHeaders.ACCESS_CONTROL_MAX_AGE, MAX_AGE);

			// OPTIONS 预检请求直接返回 200，不转发到下游服务
			if (request.getMethod() == HttpMethod.OPTIONS) {
				response.setStatusCode(HttpStatus.OK);
				return Mono.empty();
			}

			return chain.filter(ctx);
		};
	}
}
