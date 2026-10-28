package com.maozi.service.config;

import com.maozi.mvc.config.rest.RestTemplate;
import com.maozi.mvc.config.rest.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 单体模式 RestTemplate HTTP 客户端配置
 * <p>
 * 通过 {@link org.springframework.boot.web.client.RestTemplateBuilder} 构建全局共用的
 * 自定义 {@link RestTemplate} 实例（链路日志增强，且响应状态码为 400 时不抛出异常，
 * 交由调用方自行处理响应内容，其余错误状态码仍按 Spring 默认行为处理）。
 * 与分布式版本不同，本配置不接入 Sentinel 流量防护。
 * </p>
 *
 * @author pengjinlong
 * @since 2026/10/12 20:06
 */
@Configuration
public class RestTemplateConfig {

    /** 构建全局共用的 RestTemplate */
    @Bean
    public RestTemplate restTemplate(org.springframework.boot.web.client.RestTemplateBuilder restTemplateBuilder) {
        return RestTemplateBuilder.buildRestTemplate(restTemplateBuilder);
    }

}
