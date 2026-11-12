package com.maozi.discovery.config;

import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.context.annotation.Configuration;

/**
 * 灰度负载均衡自动配置
 * <p>
 * 通过 {@link LoadBalancerClients} 注解将所有 Spring Cloud LoadBalancer 客户端
 * 的默认配置替换为 {@link GrayLoadBalancerClientConfiguration}，
 * 使所有服务调用默认使用灰度轮询负载均衡策略。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Configuration
@LoadBalancerClients(defaultConfiguration = GrayLoadBalancerClientConfiguration.class)
public class GrayLoadBalancerAutoConfiguration {

}
