package com.maozi.gateway.filter;

import com.maozi.common.ObjectUtil;
import com.maozi.discovery.config.GrayRoundRobinLoadBalancer;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.DefaultRequest;
import org.springframework.cloud.client.loadbalancer.LoadBalancerUriTools;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.gateway.config.GatewayLoadBalancerProperties;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.ReactiveLoadBalancerClientFilter;
import org.springframework.cloud.gateway.support.DelegatingServiceInstance;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_SCHEME_PREFIX_ATTR;


/**
 * 灰度发布响应式负载均衡客户端过滤器。
 * <p>
 * 继承 Spring Cloud Gateway 的 ReactiveLoadBalancerClientFilter，
 * 替换默认的负载均衡策略为 {@link GrayRoundRobinLoadBalancer}（灰度轮询负载均衡器）。
 * 通过读取请求头中的灰度标识，将请求路由到对应的灰度服务实例。
 * </p>
 * <p>
 * 未重写 getOrder()，沿用父类的过滤器顺序常量
 * {@code ReactiveLoadBalancerClientFilter#LOAD_BALANCER_CLIENT_FILTER_ORDER}（10150），
 * 在路由转发（如 NettyRoutingFilter）之前完成服务实例选择。
 * </p>
 *
 * @author maozi
 */
@Component
public class GrayReactiveLoadBalancerClientFilter extends ReactiveLoadBalancerClientFilter {

    /** 负载均衡客户端工厂，用于获取服务实例列表 */
    private final LoadBalancerClientFactory grayClientFactory;

    /**
     * 构造灰度负载均衡过滤器。
     *
     * @param clientFactory 负载均衡客户端工厂
     * @param properties    网关负载均衡属性配置
     */
    public GrayReactiveLoadBalancerClientFilter(LoadBalancerClientFactory clientFactory, GatewayLoadBalancerProperties properties) {
        super(clientFactory, properties);
        this.grayClientFactory = clientFactory;
    }


    /**
     * 过滤器核心逻辑。
     * <p>
     * 1. 判断请求是否为负载均衡路由（lb 协议），非 lb 路由直接放行；
     * 2. 记录原始请求 URL；
     * 3. 使用灰度负载均衡器选择服务实例；
     * 4. 根据选中的实例重构请求 URL；
     * 5. 将重构后的 URL 设置到 Exchange 属性中。
     * </p>
     *
     * @param exchange 服务端 Web 交换上下文
     * @param chain    网关过滤器链
     * @return 过滤器链执行结果
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        // 获取当前请求的 URL
        URI url = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);

        // 获取路由方案前缀
        String schemePrefix = exchange.getAttribute(GATEWAY_SCHEME_PREFIX_ATTR);

        // 非 lb（负载均衡）协议的请求直接放行
        if (ObjectUtil.isNullEmpty(url) || (!"lb".equals(url.getScheme()) && !"lb".equals(schemePrefix))) {
            return chain.filter(exchange);
        }

        // 保存原始请求 URL，用于调试和日志
        ServerWebExchangeUtils.addOriginalRequestUrl(exchange, url);

        // 使用灰度负载均衡器选择服务实例
        return this.choose(exchange).doOnNext((response) -> {

            // 如果没有可用的服务实例，抛出 NotFoundException
            if (!response.hasServer()) {
                throw NotFoundException.create(true, "服务不存在");
            }

            URI uri = exchange.getRequest().getURI();

            // 协议覆盖方案
            String overrideScheme = null;
            if (schemePrefix != null) {
                overrideScheme = url.getScheme();
            }

            // 创建委托服务实例
            DelegatingServiceInstance serviceInstance = new DelegatingServiceInstance(response.getServer(), overrideScheme);

            // 根据服务实例信息重构请求 URI
            URI requestUrl = this.reconstructURI(serviceInstance, uri);

            // 将重构后的请求 URL 设置到 Exchange 属性中
            exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR, requestUrl);

        }).then(chain.filter(exchange));

    }

    /**
     * 使用灰度轮询负载均衡器选择服务实例。
     * <p>
     * 根据请求 URL 中的主机名（即服务 ID）获取对应的服务实例列表供应器，
     * 创建 {@link GrayRoundRobinLoadBalancer} 实例并执行选择操作。
     * </p>
     *
     * @param exchange 服务端 Web 交换上下文
     * @return 包含选中服务实例的响应 Mono
     */
    private Mono<Response<ServiceInstance>> choose(ServerWebExchange exchange) {
        URI uri = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
        GrayRoundRobinLoadBalancer loadBalancer = null;
        if (uri != null) {
            loadBalancer = new GrayRoundRobinLoadBalancer(grayClientFactory.getLazyProvider(uri.getHost(), ServiceInstanceListSupplier.class));
        }
        if (loadBalancer != null) {
            return loadBalancer.choose(this.createRequest(exchange));
        }
        return Mono.empty();
    }

    /**
     * 创建负载均衡请求对象。
     * <p>
     * 将 HTTP 请求头封装为 DefaultRequest，传递给负载均衡器，
     * 灰度负载均衡器可根据请求头中的灰度标识进行路由决策。
     * </p>
     *
     * @param exchange 服务端 Web 交换上下文
     * @return 包含 HTTP 请求头的负载均衡请求
     */
    private Request<?> createRequest(ServerWebExchange exchange) {
        HttpHeaders headers = exchange.getRequest().getHeaders();
        return new DefaultRequest<>(headers);
    }

    /**
     * 根据服务实例重构请求 URI。
     * <p>
     * 将原始请求的 URI 中的主机和端口替换为实际的服务实例地址。
     * </p>
     *
     * @param serviceInstance 目标服务实例
     * @param original        原始请求 URI
     * @return 重构后的请求 URI
     */
    protected URI reconstructURI(ServiceInstance serviceInstance, URI original) {
        return LoadBalancerUriTools.reconstructURI(serviceInstance, original);
    }

}
