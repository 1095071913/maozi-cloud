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


package com.maozi.gateway.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.result.view.ViewResolver;

import java.util.Collections;
import java.util.List;

/**
 * 网关异常处理配置类。
 * <p>
 * 注册自定义的全局异常处理器 {@link GatewayExceptionHandler}，
 * 替换 Spring WebFlux 默认的异常处理逻辑，统一网关层错误响应格式。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Configuration
public class GatewayExceptionConfig {

    /**
     * 注册自定义异常处理器 Bean。
     * <p>
     * 使用 @Primary 注解覆盖默认的 ErrorWebExceptionHandler，
     * 并设置最高优先级确保在其他处理器之前执行。
     * 同时注入视图解析器、消息读写器供异常响应渲染使用。
     * </p>
     *
     * @param viewResolversProvider  视图解析器列表提供者
     * @param serverCodecConfigurer  HTTP 消息编解码器配置
     * @return 自定义的网关异常处理器
     */
    @Primary
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public ErrorWebExceptionHandler errorWebExceptionHandler(ObjectProvider<List<ViewResolver>> viewResolversProvider, ServerCodecConfigurer serverCodecConfigurer) {
    	GatewayExceptionHandler gatewayExceptionHandler = new GatewayExceptionHandler();
    	gatewayExceptionHandler.setViewResolvers(viewResolversProvider.getIfAvailable(Collections::emptyList));
    	gatewayExceptionHandler.setMessageWriters(serverCodecConfigurer.getWriters());
    	gatewayExceptionHandler.setMessageReaders(serverCodecConfigurer.getReaders());
        return gatewayExceptionHandler;
    }

}
