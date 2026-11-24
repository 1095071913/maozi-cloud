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
package com.alibaba.csp.sentinel.dashboard.auth;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.util.AntPathMatcher;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * <p>用于登录认证的 Servlet 过滤器。</p>
 *
 * <p>注意：部分 URL 无需认证，直接放行，包括：</p>
 * <ul>
 * <li>首页地址：{@code /}</li>
 * <li>认证请求地址：{@code /login}、{@code /logout}</li>
 * <li>机器注册地址：{@code /registry/machine}</li>
 * <li>静态资源</li>
 * </ul>
 * <p>
 * 排除的 URL 与后缀均可在 {@code application.properties} 中配置。
 *
 * @author cdfive
 * @date 2026/11/22
 * @since 1.6.0
 */
public class DefaultLoginAuthenticationFilter implements LoginAuthenticationFilter {

    /** 用于匹配 Servlet 路径与排除 URL 模式的 Ant 风格路径匹配器。 */
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    /** URL 后缀的点号前缀，配置值中无需再带点号。 */
    private static final String URL_SUFFIX_DOT = ".";

    /** 无需认证的 URL 模式列表，如 /auth/login、/registry/machine。 */
    @Value("#{'${auth.filter.exclude-urls}'.split(',')}")
    private List<String> authFilterExcludeUrls;

    /** 无需认证的 URL 后缀列表，如 htm、html、js。 */
    @Value("#{'${auth.filter.exclude-url-suffixes}'.split(',')}")
    private List<String> authFilterExcludeUrlSuffixes;

    /** 用于校验登录请求的认证服务。 */
    private final AuthService<HttpServletRequest> authService;

    /** 使用指定的认证服务构建过滤器。 */
    public DefaultLoginAuthenticationFilter(AuthService<HttpServletRequest> authService) {
        this.authService = authService;
    }

    /** {@inheritDoc} 空实现：无需初始化。 */
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    /**
     * 匹配排除 URL 模式或后缀的请求直接放行；其余请求经 {@link AuthService} 认证，
     * 认证失败时响应 401 状态码。
     *
     * @param request  当前请求
     * @param response 当前响应
     * @param chain    过滤器链
     * @throws IOException      IO 异常
     * @throws ServletException Servlet 异常
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        String servletPath = httpRequest.getServletPath();

        // 命中无需认证的 URL 模式时直接放行
        boolean authFilterExcludeMatch = authFilterExcludeUrls.stream()
                .anyMatch(authFilterExcludeUrl -> PATH_MATCHER.match(authFilterExcludeUrl, servletPath));
        if (authFilterExcludeMatch) {
            chain.doFilter(request, response);
            return;
        }

        // 命中无需认证的 URL 后缀时直接放行
        for (String authFilterExcludeUrlSuffix : authFilterExcludeUrlSuffixes) {
            if (StringUtils.isBlank(authFilterExcludeUrlSuffix)) {
                continue;
            }

            // 自动补全后缀前的点号，配置文件中无需书写
            if (!authFilterExcludeUrlSuffix.startsWith(URL_SUFFIX_DOT)) {
                authFilterExcludeUrlSuffix = URL_SUFFIX_DOT + authFilterExcludeUrlSuffix;
            }

            if (servletPath.endsWith(authFilterExcludeUrlSuffix)) {
                chain.doFilter(request, response);
                return;
            }
        }

        AuthService.AuthUser authUser = authService.getAuthUser(httpRequest);

        HttpServletResponse httpResponse = (HttpServletResponse) response;
        if (authUser == null) {
            // 认证失败，响应 401 状态码
            httpResponse.setStatus(HttpStatus.UNAUTHORIZED.value());
        } else {
            chain.doFilter(request, response);
        }
    }

    /** {@inheritDoc} 空实现：无需释放资源。 */
    @Override
    public void destroy() {}
}
