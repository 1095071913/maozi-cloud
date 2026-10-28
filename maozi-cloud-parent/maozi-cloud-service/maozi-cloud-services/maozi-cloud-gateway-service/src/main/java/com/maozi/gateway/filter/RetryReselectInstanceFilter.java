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

    /**
     * 返回过滤器执行顺序
     * <p>
     * 取 10140：必须小于 ReactiveLoadBalancerClientFilter 的 10150，
     * 保证负载均衡解析地址之前能将地址恢复为 lb://；同时大于 Retry 的 2，
     * 保证重试进入本过滤器时仍可执行恢复逻辑。
     * </p>
     *
     * @return 过滤器顺序值
     */
    @Override
    public int getOrder() {
        return 10140;
    }

    /**
     * 重试时将请求地址恢复为原始 lb:// 地址
     * <p>
     * 首次请求（地址 scheme 为 lb）时记录原始 lb 地址；重试时地址已被负载均衡
     * 过滤器替换为具体节点地址（如 http://A），此处恢复为记录的 lb:// 地址，
     * 使后续的负载均衡过滤器重新选择节点，实现 A 不可用时重试到 B。
     * </p>
     *
     * @param exchange 当前请求上下文
     * @param chain 过滤器链
     * @return 过滤器链执行结果
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        URI url = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);

        if (url != null && "lb".equals(url.getScheme())) {
            // 首次尝试：LB 还没解析，记录 lb 地址
            exchange.getAttributes().put(ORIGINAL_LB_URL_ATTR, url);

        } else if (url != null && exchange.getAttribute(ORIGINAL_LB_URL_ATTR) != null) {
            // 重试：地址已被 LB 替换成具体节点(http://A)，恢复成 lb://，
            // 后续 LB 过滤器会重新 choose() → RoundRobin 轮到下一个节点
            exchange.getAttributes().put(
                    ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR,
                    exchange.getAttribute(ORIGINAL_LB_URL_ATTR)
            );
        }
        return chain.filter(exchange);
    }
}
