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
 * Spring configuration that registers the auth-related beans: an {@link AuthService},
 * a login authentication filter and an authorization interceptor, each overridable
 * by the user's own bean definition.
 */
@Configuration
@EnableConfigurationProperties(AuthProperties.class)
public class AuthConfiguration {

    /** The auth configuration properties. */
    private final AuthProperties authProperties;

    /** Creates the configuration with the bound auth properties. */
    public AuthConfiguration(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    /** Provides the auth service: session-based when auth is enabled, otherwise a fake one that passes all checking. */
    @Bean
    @ConditionalOnMissingBean
    public AuthService<HttpServletRequest> httpServletRequestAuthService() {
        if (this.authProperties.isEnabled()) {
            return new SimpleWebAuthServiceImpl();
        }
        return new FakeAuthServiceImpl();
    }

    /** Provides the default login authentication filter backed by the given auth service. */
    @Bean
    @ConditionalOnMissingBean
    public LoginAuthenticationFilter loginAuthenticationFilter(AuthService<HttpServletRequest> httpServletRequestAuthService) {
        return new DefaultLoginAuthenticationFilter(httpServletRequestAuthService);
    }

    /** Provides the default privilege-based authorization interceptor backed by the given auth service. */
    @Bean
    @ConditionalOnMissingBean
    public AuthorizationInterceptor authorizationInterceptor(AuthService<HttpServletRequest> httpServletRequestAuthService) {
        return new DefaultAuthorizationInterceptor(httpServletRequestAuthService);
    }

}
