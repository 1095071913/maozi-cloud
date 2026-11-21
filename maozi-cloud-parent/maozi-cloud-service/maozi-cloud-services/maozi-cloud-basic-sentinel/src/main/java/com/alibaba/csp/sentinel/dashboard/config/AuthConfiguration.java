/*
 * Copyright 1999-2018 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.csp.sentinel.dashboard.config;

import com.alibaba.csp.sentinel.dashboard.auth.AuthService;
import com.alibaba.csp.sentinel.dashboard.auth.AuthorizationInterceptor;
import com.alibaba.csp.sentinel.dashboard.auth.DefaultAuthorizationInterceptor;
import com.alibaba.csp.sentinel.dashboard.auth.DefaultLoginAuthenticationFilter;
import com.alibaba.csp.sentinel.dashboard.auth.FakeAuthServiceImpl;
import com.alibaba.csp.sentinel.dashboard.auth.LoginAuthenticationFilter;
import com.alibaba.csp.sentinel.dashboard.auth.SimpleWebAuthServiceImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.servlet.http.HttpServletRequest;

/**
 * 注册认证相关 Bean 的 Spring 配置：{@link AuthService}、登录认证过滤器和授权拦截器，
 * 各 Bean 均可被用户自定义的同名 Bean 覆盖。
 * @author maozi
 * @date 2026/11/22
 */
@Configuration
@EnableConfigurationProperties(AuthProperties.class)
public class AuthConfiguration {

    /** 认证配置属性。 */
    private final AuthProperties authProperties;

    /** 使用绑定的认证配置属性构建配置类。 */
    public AuthConfiguration(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    /** 提供认证服务：开启认证时为基于 session 的实现，否则为跳过全部校验的伪实现。 */
    @Bean
    @ConditionalOnMissingBean
    public AuthService<HttpServletRequest> httpServletRequestAuthService() {
        if (this.authProperties.isEnabled()) {
            return new SimpleWebAuthServiceImpl();
        }
        return new FakeAuthServiceImpl();
    }

    /** 提供基于指定认证服务的默认登录认证过滤器。 */
    @Bean
    @ConditionalOnMissingBean
    public LoginAuthenticationFilter loginAuthenticationFilter(AuthService<HttpServletRequest>
        httpServletRequestAuthService) {
        return new DefaultLoginAuthenticationFilter(httpServletRequestAuthService);
    }

    /** 提供基于指定认证服务的默认权限校验拦截器。 */
    @Bean
    @ConditionalOnMissingBean
    public AuthorizationInterceptor authorizationInterceptor(AuthService<HttpServletRequest>
        httpServletRequestAuthService) {
        return new DefaultAuthorizationInterceptor(httpServletRequestAuthService);
    }

}
