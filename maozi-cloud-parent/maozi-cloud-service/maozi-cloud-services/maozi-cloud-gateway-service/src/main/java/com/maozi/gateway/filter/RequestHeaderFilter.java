package com.maozi.gateway.filter;

import com.maozi.common.context.ApplicationLinkContext;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 请求头安全过滤全局过滤器。
 * <p>
 * 网关作为系统对外的唯一入口，必须保证内部服务间传递的信任请求头
 * 不被外部客户端伪造。{@link ApplicationLinkContext#CURRENT_USER_INFO_KEY}
 * （X-CurrentUserInfo）用于内部服务（Feign/Dubbo）之间传递登录用户身份，
 * 若允许外部请求直接携带该请求头，将造成用户身份伪造风险。
 * </p>
 * <p>
 * 本过滤器在请求进入网关的第一时间移除该请求头，
 * 确保下游服务只能通过网关鉴权后获取真实的用户身份信息。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
@Component
public class RequestHeaderFilter implements GlobalFilter, Ordered {

	/**
	 * 过滤器核心逻辑。
	 * <p>
	 * 通过 {@link ServerWebExchange#mutate()} 重建请求，
	 * 移除原始请求中的 {@link ApplicationLinkContext#CURRENT_USER_INFO_KEY} 请求头，
	 * 随后继续执行过滤器链。
	 * </p>
	 *
	 * @param exchange 服务端 Web 交换上下文
	 * @param chain    网关过滤器链
	 * @return 过滤器链执行结果
	 */
	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

		// 重建请求，移除携带登录用户身份的请求头，防止外部伪造
		ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
			.headers(headers -> headers.remove(ApplicationLinkContext.CURRENT_USER_INFO_KEY))
			.build();

		return chain.filter(exchange.mutate().request(mutatedRequest).build());

	}

	/**
	 * 获取过滤器执行顺序。
	 * <p>
	 * 设置为 HIGHEST_PRECEDENCE，确保在所有其他过滤器之前移除敏感请求头，
	 * 避免后续过滤器或日志记录到伪造的用户身份信息。
	 * </p>
	 *
	 * @return 过滤器顺序值
	 */
	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}

}
