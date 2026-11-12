package com.maozi.service.config;

import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 请求首段路径裁剪过滤器
 * <p>
 * 聚合服务（maozi-cloud-all-service）将 oauth、system 等多个子模块聚合在同一进程内，
 * 各子模块控制器并不携带模块前缀
 * （声明的路径是 {@code /user/list} 而不是 {@code /system/user/list}）。
 * 外部网关若按 {@code /<module>/<path>} 形式路由进入本服务，则需要在 Spring MVC
 * 控制器匹配前裁剪掉第一段路径，将 {@code /oauth/user/info} 改写为 {@code /user/info}，
 * 否则无法命中控制器。
 * </p>
 *
 * <p><b>实现方式：</b>因为 Spring MVC 的 {@code HandlerInterceptor} 在控制器已被解析后
 * 才执行，无法改变用于匹配的路径，所以本组件以 Servlet Filter 形式实现，并通过
 * {@link HttpServletRequestWrapper} 重写 {@code getRequestURI()}、{@code getServletPath()}
 * 与 {@code getRequestURL()}，使 Spring 6 的 {@code ServletRequestPathUtils.parseAndCache}
 * 解析到裁剪后的路径，再由 {@code RequestMappingHandlerMapping} 用新路径匹配控制器。</p>
 *
 * <p><b>执行顺序：</b>过滤器 order 设为 {@code SecurityProperties.DEFAULT_FILTER_ORDER - 1}（-101），
 * 早于 Spring Security 过滤器链（{@code SecurityProperties.DEFAULT_FILTER_ORDER = -100}），确保：
 * <ul>
 *   <li>带模块前缀的请求（如 {@code /oauth/oauth2/token}、{@code /oauth/user/info}）在进入安全过滤器链之前
 *       被裁剪掉首段模块前缀，使 Spring Security 的授权服务器过滤器链能正确匹配 {@code /oauth2/token}
 *       等框架端点，模块控制器的安全校验与 MVC 匹配也能使用裁剪后的路径；</li>
 *   <li>OAuth2/OIDC 框架端点（{@code /oauth2}、{@code /.well-known}）及
 *       Spring Boot 框架端点（{@code /actuator}、{@code /webjars} 等）通过排除列表保留原始路径，
 *       安全白名单匹配仍基于这些原始路径。</li>
 * </ul>
 * </p>
 *
 * <p><b>路径排除：</b>默认对以下框架端点不做裁剪：
 * Spring Boot 框架端点（{@code /actuator}、{@code /webjars}、{@code /v3}、{@code /application}、
 * {@code /error}、{@code /favicon.ico}），以及 OAuth2/OIDC 框架端点（{@code /oauth2}、
 * {@code /.well-known}），避免破坏 Actuator 监控、Swagger 静态资源、OpenAPI 文档、
 * OAuth2 授权服务器端点等。可通过配置 {@code application.path-strip.excluded-prefixes} 覆盖默认排除列表；
 * 如需完全关闭裁剪，可设置 {@code application.path-strip.enabled=false}。</p>
 *
 * <p><b>注：</b>{@code /userinfo} 端点（OIDC UserInfo Endpoint）当前并未包含在
 * 默认排除列表中，若依赖该端点请通过 {@code application.path-strip.excluded-prefixes} 显式补充。</p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Component
@Order(SecurityProperties.DEFAULT_FILTER_ORDER - 1)
public class StripFirstPathFilter extends OncePerRequestFilter {

    /** 默认不裁剪的路径前缀（框架端点），可通过配置覆盖 */
    private static final String DEFAULT_EXCLUDED_PREFIXES = "/actuator,/webjars,/v3,/application,/error,/favicon.ico,/oauth2,/.well-known";

    /** 是否启用路径裁剪，默认开启 */
    @Value("${application.path-strip.enabled:true}")
    private boolean enabled;

    /** 自定义排除的前缀列表，逗号分隔 */
    @Value("${application.path-strip.excluded-prefixes:" + DEFAULT_EXCLUDED_PREFIXES + "}")
    private List<String> excludedPrefixes;

    /**
     * 包装请求并继续过滤器链
     * <p>
     * 计算应用内相对路径（去掉 contextPath），若命中排除列表则原样放行，
     * 否则用 {@link StrippedRequestWrapper} 包装请求，让后续 Spring MVC 解析到裁剪后的路径。
     * </p>
     *
     * @param request     原始 HTTP 请求
     * @param response    HTTP 响应
     * @param filterChain 过滤器链
     * @throws ServletException Servlet 异常
     * @throws IOException      IO 异常
     */
    @Override
    protected void doFilterInternal(@Nonnull HttpServletRequest request, @Nonnull HttpServletResponse response, @Nonnull FilterChain filterChain) throws ServletException, IOException {

        // 未启用裁剪功能，直接放行
        if (!enabled) {
            filterChain.doFilter(request, response);
            return;
        }

        // 计算 contextPath 之后的相对路径，作为裁剪判断与计算的基础
        String contextPath = request.getContextPath();
        String requestUri = request.getRequestURI();
        String pathWithinApp = requestUri.startsWith(contextPath) ? requestUri.substring(contextPath.length()) : requestUri;

        // 命中排除列表（框架端点等）则保持原路径
        if (!shouldStrip(pathWithinApp)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 用包装器替换请求，使后续 DispatcherServlet 解析到裁剪后的路径
        filterChain.doFilter(new StrippedRequestWrapper(request, contextPath, pathWithinApp), response);

    }

    /**
     * 判断应用内相对路径是否需要裁剪第一段
     * <p>
     * 路径为空或仅 {@code /} 时不裁剪；命中任一排除前缀（精确匹配或前缀 + "/"）也不裁剪。
     * </p>
     *
     * @param pathWithinApp 应用内相对路径
     * @return true 表示需要裁剪第一段；false 表示保持原样
     */
    private boolean shouldStrip(String pathWithinApp) {

        // 路径为空或仅 "/"，无法裁剪
        if (pathWithinApp == null || pathWithinApp.length() <= 1) {
            return false;
        }

        // 遍历排除前缀，命中则不裁剪
        for (String prefix : excludedPrefixes) {
            String normalized = prefix.startsWith("/") ? prefix : "/" + prefix;
            // 匹配 /prefix 本身或 /prefix/... 子路径
            if (pathWithinApp.equals(normalized) || pathWithinApp.startsWith(normalized + "/")) {
                return false;
            }
        }

        return true;

    }

    /**
     * 裁剪请求首段路径的请求包装器
     * <p>
     * 重写 URI 相关方法，让 Spring MVC 路径解析机制拿到去掉首段后的新路径。
     * Spring 6 中 {@code DispatcherServlet} 会调用 {@code ServletRequestPathUtils.parseAndCache}
     * 对 {@code getRequestURI()} 进行解析，因此重写该方法即可让控制器匹配使用新路径。
     * </p>
     */
    private static class StrippedRequestWrapper extends HttpServletRequestWrapper {

        /** 原始请求 URI，用于重写 getRequestURL 时定位替换位置 */
        private final String originalRequestUri;

        /** 裁剪后的新请求 URI（含 contextPath） */
        private final String newRequestUri;

        /** 裁剪后的新 servlet 路径（contextPath 之后的部分） */
        private final String newServletPath;

        /** 原始请求 URL，作为重建 getRequestURL 的基础 */
        private final StringBuffer originalRequestUrl;

        /**
         * 构造方法
         *
         * @param request       原始请求
         * @param contextPath   应用上下文路径
         * @param pathWithinApp 应用内相对路径（contextPath 之后的部分）
         */
        StrippedRequestWrapper(HttpServletRequest request, String contextPath, String pathWithinApp) {
            super(request);
            this.originalRequestUri = request.getRequestURI();
            this.originalRequestUrl = request.getRequestURL();
            String stripped = stripFirstSegment(pathWithinApp);
            this.newRequestUri = contextPath + stripped;
            this.newServletPath = stripped;
        }

        /**
         * 裁剪路径首段
         * <p>
         * 例如 {@code /oauth/user/info} → {@code /user/info}；
         * {@code /oauth} → {@code /}；{@code /} → {@code /}。
         * </p>
         *
         * @param path 原始路径
         * @return 裁剪首段后的路径；若仅有一段或为空则返回 {@code /}
         */
        private static String stripFirstSegment(String path) {
            if (path == null || path.length() <= 1) {
                return "/";
            }
            int secondSlash = path.indexOf('/', 1);
            return secondSlash < 0 ? "/" : path.substring(secondSlash);
        }

        /**
         * 重写为裁剪后的请求 URI
         *
         * @return 裁剪后的新请求 URI（含 contextPath）
         */
        @Override
        public String getRequestURI() {
            return newRequestUri;
        }

        /**
         * 重写为裁剪后的 servlet 路径
         *
         * @return 裁剪后的新 servlet 路径（contextPath 之后的部分）
         */
        @Override
        public String getServletPath() {
            return newServletPath;
        }

        /**
         * 基于原始 URL 重建裁剪后的完整请求 URL
         *
         * @return 裁剪后的完整请求 URL（scheme、host、port 保持不变；原始 URI 未命中时返回原始 URL）
         */
        @Override
        public StringBuffer getRequestURL() {
            StringBuffer url = new StringBuffer(originalRequestUrl);
            int idx = url.indexOf(originalRequestUri);
            if (idx >= 0) {
                url.replace(idx, idx + originalRequestUri.length(), newRequestUri);
            }
            return url;
        }

    }

}
