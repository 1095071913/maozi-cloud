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

import com.alibaba.csp.sentinel.adapter.servlet.CommonFilter;
import com.alibaba.csp.sentinel.adapter.servlet.callback.WebCallbackManager;
import com.alibaba.csp.sentinel.dashboard.auth.AuthorizationInterceptor;
import com.alibaba.csp.sentinel.dashboard.auth.LoginAuthenticationFilter;
import com.alibaba.csp.sentinel.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.PostConstruct;
import javax.servlet.Filter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Web MVC 配置：注册授权拦截器、登录认证过滤器和 Sentinel {@link CommonFilter}，
 * 并映射静态资源与首页视图。
 *
 * @author leyou
 * @date 2026/11/22
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /** 全部 URL 的路径模式。 */
    private static final String ALL_URL_PATTERN = "/*";

    /** 登录认证过滤器的注册顺序，需先于 Sentinel 过滤器执行。 */
    private static final int AUTHENTICATION_FILTER_ORDER = 0;

    /** Sentinel CommonFilter 的注册顺序。 */
    private static final int SENTINEL_FILTER_ORDER = 1;

    /** 不纳入 Sentinel Web 统计的静态资源 URL 后缀。 */
    private static final Set<String> STATIC_RESOURCE_SUFFIXES = new HashSet<>(Arrays.asList(
            ".js", ".css", ".html", ".ico", ".txt", ".woff", ".woff2"));

    /** 日志记录器。 */
    private static final Logger logger = LoggerFactory.getLogger(WebConfig.class);

    /** 待注册的登录认证过滤器。 */
    private final LoginAuthenticationFilter loginAuthenticationFilter;

    /** 待注册的授权拦截器。 */
    private final AuthorizationInterceptor authorizationInterceptor;

    /** 注入登录认证过滤器与授权拦截器。 */
    public WebConfig(LoginAuthenticationFilter loginAuthenticationFilter,
            AuthorizationInterceptor authorizationInterceptor) {
        this.loginAuthenticationFilter = loginAuthenticationFilter;
        this.authorizationInterceptor = authorizationInterceptor;
    }

    /** 为所有路径注册授权拦截器。 */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authorizationInterceptor).addPathPatterns("/**");
    }

    /** 映射 classpath:/resources/ 下的静态资源，每次请求强制重新校验新鲜度（no-cache）。 */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 前端为懒加载 SPA（视图与控制器脚本通过 XHR 拉取）：
        // 若被浏览器缓存，重新发布后页面会继续使用旧的 JS/HTML，
        // 因此禁用缓存并通过每次请求的 304 协商重新校验新鲜度。
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/resources/")
                .setCacheControl(CacheControl.noCache());
    }

    /** 将根路径 "/" 转发到 /index.htm。 */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/index.htm");
    }

    /**
     * 为全部 URL 注册 Sentinel {@link CommonFilter}，这是将 Sentinel
     * 接入 Web 应用的最简方式。
     *
     * @return 过滤器注册 Bean
     */
    @Bean
    public FilterRegistrationBean<Filter> sentinelFilterRegistration() {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new CommonFilter());
        registration.addUrlPatterns(ALL_URL_PATTERN);
        registration.setName("sentinelFilter");
        registration.setOrder(SENTINEL_FILTER_ORDER);
        // 开启后所有 Web URL 资源的入口将统一为同一个 context 名，
        // 绝大多数场景已足够，且可降低内存占用。
        registration.addInitParameter(CommonFilter.WEB_CONTEXT_UNIFY, "true");

        logger.info("Sentinel servlet CommonFilter registered");

        return registration;
    }

    /** 注册 UrlCleaner，按后缀将常见静态资源 URL 排除出 Sentinel Web 统计。 */
    @PostConstruct
    public void doInit() {
        WebCallbackManager.setUrlCleaner(url -> {
            if (StringUtil.isEmpty(url)) {
                return url;
            }
            if (STATIC_RESOURCE_SUFFIXES.stream().anyMatch(url::endsWith)) {
                return null;
            }
            return url;
        });
    }

    /** 为全部 URL 注册登录认证过滤器，order 为 0，先于 Sentinel CommonFilter 执行。 */
    @Bean
    public FilterRegistrationBean authenticationFilterRegistration() {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter(loginAuthenticationFilter);
        registration.addUrlPatterns(ALL_URL_PATTERN);
        registration.setName("authenticationFilter");
        registration.setOrder(AUTHENTICATION_FILTER_ORDER);
        return registration;
    }
}
