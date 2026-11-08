package com.maozi.discovery.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClientConfiguration;
import org.springframework.cloud.loadbalancer.core.ReactorLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * 灰度负载均衡客户端配置
 * <p>
 * 继承 Spring Cloud 的 {@link LoadBalancerClientConfiguration}，
 * 将默认的负载均衡器替换为 {@link GrayRoundRobinLoadBalancer}，
 * 实现基于版本号的灰度流量路由。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
public class GrayLoadBalancerClientConfiguration extends LoadBalancerClientConfiguration {

    /**
     * 创建灰度轮询负载均衡器
     *
     * @param environment Spring 环境变量
     * @param loadBalancerClientFactory 负载均衡客户端工厂
     * @return 灰度轮询负载均衡器实例
     */
    @Bean
    @Override
    @ConditionalOnMissingBean
    public ReactorLoadBalancer<ServiceInstance> reactorServiceInstanceLoadBalancer(Environment environment,LoadBalancerClientFactory loadBalancerClientFactory) {

        // 从环境属性 loadbalancer.client.name 获取目标服务名
        String name = environment.getProperty(LoadBalancerClientFactory.PROPERTY_NAME);

        return new GrayRoundRobinLoadBalancer(loadBalancerClientFactory.getLazyProvider(name, ServiceInstanceListSupplier.class));

    }

}
