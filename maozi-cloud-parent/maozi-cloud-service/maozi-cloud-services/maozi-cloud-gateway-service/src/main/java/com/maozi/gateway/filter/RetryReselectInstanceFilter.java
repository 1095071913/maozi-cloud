package com.maozi.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;

/**
 * 重试时恢复 lb:// 地址，让 ReactiveLoadBalancerClientFilter 重新选节点（轮询），
 * 实现 A 不可用时重试到 B。
 */
@Component
public class RetryReselectInstanceFilter implements GlobalFilter, Ordered {

    /** 本 exchange 首次解析前的 lb 地址 */
    public static final String ORIGINAL_LB_URL_ATTR = "gatewayOriginalLbUrl";

    @Override
    public int getOrder() {
        // 必须小于 ReactiveLoadBalancerClientFilter 的 10150，且大于 Retry(2)
        return 10140;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        URI url = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);

        if (url != null && "lb".equals(url.getScheme())) {
            // 首次尝试：LB 还没解析，记录 lb 地址
            exchange.getAttributes().put(ORIGINAL_LB_URL_ATTR, url);
        }
        else if (url != null && exchange.getAttribute(ORIGINAL_LB_URL_ATTR) != null) {
            // 重试：地址已被 LB 替换成具体节点(http://A)，恢复成 lb://，
            // 后续 LB 过滤器会重新 choose() → RoundRobin 轮到下一个节点
            exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR,
                    exchange.getAttribute(ORIGINAL_LB_URL_ATTR));
        }
        return chain.filter(exchange);
    }
}
