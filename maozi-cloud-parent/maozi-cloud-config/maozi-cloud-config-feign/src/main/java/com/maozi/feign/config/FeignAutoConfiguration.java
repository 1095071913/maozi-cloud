package com.maozi.feign.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

/**
 * Feign 自动配置
 * <p>
 * 通过 {@link EnableFeignClients} 开启 OpenFeign 声明式 HTTP 客户端能力，
 * 并指定扫描 {@code com.maozi.*.*.api.rest} 包下的 {@code @FeignClient} 接口
 * （即各服务 REST API 模块中声明的 Feign 客户端）。
 * </p>
 * <p>
 * 由于本配置类位于 {@code com.maozi.feign.config} 包，若不显式指定 {@link EnableFeignClients#basePackages()}，
 * 默认仅会扫描本类所在包及其子包，导致其它模块（如 {@code com.maozi.oauth.*}）中声明的
 * Feign 客户端无法被注册，故此处必须明确指定基础包。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
@Configuration
@EnableFeignClients(basePackages = "com.maozi.*.*.api.rest")
public class FeignAutoConfiguration {

}
