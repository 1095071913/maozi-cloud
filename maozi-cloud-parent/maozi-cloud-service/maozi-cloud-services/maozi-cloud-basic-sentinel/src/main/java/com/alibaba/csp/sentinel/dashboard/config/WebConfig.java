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
import org.springframework.beans.factory.annotation.Autowired;
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
 * Web MVC configuration: registers the authorization interceptor, the login
 * authentication filter and the Sentinel {@link CommonFilter}, and maps static
 * resources and the index view.
 *
 * @author leyou
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /** Logger of this configuration. */
    private final Logger logger = LoggerFactory.getLogger(WebConfig.class);

    /** The login authentication filter to register. */
    @Autowired
    private LoginAuthenticationFilter loginAuthenticationFilter;

    /** The authorization interceptor to register. */
    @Autowired
    private AuthorizationInterceptor authorizationInterceptor;

    /** Register the authorization interceptor for all paths. */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authorizationInterceptor).addPathPatterns("/**");
    }

    /** Serves static resources under classpath:/resources/, forcing revalidation (no-cache) on each request. */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 前端为 SPA 懒加载(视图模板/控制器脚本经 XHR 拉取), 若被浏览器缓存,
        // 重新部署后页面会继续使用旧版 JS/HTML 导致功能异常, 因此禁用缓存,
        // 浏览器每次通过 304 协商缓存校验新鲜度
        registry.addResourceHandler("/**")
            .addResourceLocations("classpath:/resources/")
            .setCacheControl(CacheControl.noCache());
    }

    /** Map the root path "/" to forward to /index.htm. */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/index.htm");
    }

    /**
     * Add {@link CommonFilter} to the server, this is the simplest way to use Sentinel
     * for Web application.
     */
    @Bean
    public FilterRegistrationBean sentinelFilterRegistration() {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new CommonFilter());
        registration.addUrlPatterns("/*");
        registration.setName("sentinelFilter");
        registration.setOrder(1);
        // If this is enabled, the entrance of all Web URL resources will be unified as a single context name.
        // In most scenarios that's enough, and it could reduce the memory footprint.
        registration.addInitParameter(CommonFilter.WEB_CONTEXT_UNIFY, "true");

        logger.info("Sentinel servlet CommonFilter registered");

        return registration;
    }

    /** Registers a UrlCleaner that excludes common static resource urls (by suffix) from Sentinel web statistics. */
    @PostConstruct
    public void doInit() {
        Set<String> suffixSet = new HashSet<>(Arrays.asList(".js", ".css", ".html", ".ico", ".txt",
            ".woff", ".woff2"));
        // Example: register a UrlCleaner to exclude URLs of common static resources.
        WebCallbackManager.setUrlCleaner(url -> {
            if (StringUtil.isEmpty(url)) {
                return url;
            }
            if (suffixSet.stream().anyMatch(url::endsWith)) {
                return null;
            }
            return url;
        });
    }

    /** Registers the login authentication filter for all urls at order 0, ahead of the Sentinel CommonFilter. */
    @Bean
    public FilterRegistrationBean authenticationFilterRegistration() {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter(loginAuthenticationFilter);
        registration.addUrlPatterns("/*");
        registration.setName("authenticationFilter");
        registration.setOrder(0);
        return registration;
    }
}
