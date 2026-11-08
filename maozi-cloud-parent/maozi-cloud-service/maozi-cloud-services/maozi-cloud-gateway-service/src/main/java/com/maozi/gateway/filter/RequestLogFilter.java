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

package com.maozi.gateway.filter;

import com.maozi.common.constant.LogTag;
import com.maozi.common.enums.LogCommonType;
import com.maozi.gateway.config.ServerHttpResponseAgent;
import com.maozi.gateway.utils.WebUtil;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 请求日志全局过滤器。
 * <p>
 * 在网关层面记录每个请求的基本信息（IP、类型、URL、方法），
 * 并通过 {@link ServerHttpResponseAgent} 代理响应对象，
 * 在响应返回时自动补充响应耗时（RT）和响应数据。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
@Component
public class RequestLogFilter implements GlobalFilter, Ordered {

	/**
	 * 过滤器核心逻辑。
	 * <p>
	 * 1. 记录请求开始时间戳；
	 * 2. 收集请求 IP、日志类型、请求 URL、HTTP 方法等基本信息；
	 * 3. 使用 {@link ServerHttpResponseAgent} 包装原始响应，
	 *    使得响应写入时自动记录日志和注入链路追踪 ID。
	 * </p>
	 *
	 * @param exchange 服务端 Web 交换上下文
	 * @param chain    网关过滤器链
	 * @return 过滤器链执行结果
	 */
	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

		// 记录请求开始时间，用于计算响应耗时
		Long requestTime = System.currentTimeMillis();

		// 构建日志信息 Map
		Map<String,String> logs = new LinkedHashMap<>();

		// 记录客户端 IP 地址
		logs.put(LogTag.IP, WebUtil.getRequestHost(exchange.getRequest()));
		// 记录日志类型（网关）
		logs.put(LogTag.TYPE, LogCommonType.GATEWAY.getDesc());
		// 记录请求 URL
		logs.put(LogTag.URL, exchange.getRequest().getURI().toString());
		// 记录 HTTP 请求方法
		logs.put(LogTag.METHOD, Objects.requireNonNull(exchange.getRequest().getMethod()).toString());

		// 使用 ServerHttpResponseAgent 包装响应，在响应写入时自动记录日志
		return chain.filter(exchange.mutate().response(new ServerHttpResponseAgent(requestTime, logs,exchange.getResponse(),exchange.getAttributes(),exchange.getRequest())).build());

	}

	/**
	 * 获取过滤器执行顺序。
	 * <p>
	 * 设置为 HIGHEST_PRECEDENCE + 1，确保在最高优先级过滤器之后执行，
	 * 但在大多数其他过滤器之前执行，以保证日志记录的完整性。
	 * </p>
	 *
	 * @return 过滤器顺序值
	 */
	@Override
	public int getOrder() {return Ordered.HIGHEST_PRECEDENCE+1;}

}
