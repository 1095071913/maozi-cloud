package com.maozi.config;

import com.maozi.common.context.ApplicationLinkContext;
import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 用户信息请求头清除过滤器
 * <p>
 * 聚合服务（maozi-cloud-all-service）作为对外入口，直接接收外部请求。
 * {@code X-CurrentUserInfo}（{@link ApplicationLinkContext#CURRENT_USER_INFO_KEY}）
 * 请求头用于服务间内部传递当前登录用户信息，不应由外部客户端直接注入，
 * 否则攻击者可伪造用户身份绕过认证。
 * </p>
 * <p>
 * 本过滤器优先级高于 Spring Security 过滤器链（{@code SecurityProperties.DEFAULT_FILTER_ORDER = -100}），
 * 即在 {@code BearerTokenAuthenticationFilter} 触发 {@code OpaqueTokenIntrospector}
 * 内省令牌之前执行，确保用户身份仅来源于令牌内省结果，而非外部请求头。
 * </p>
 * <p>
 * 由于 {@link com.maozi.mvc.filter.ApplicationContextFilter} 以 {@code HIGHEST_PRECEDENCE}
 * 执行并已从该请求头写入 {@link ApplicationLinkContext#currentUserInfos} 线程本地变量，
 * 本过滤器在 Security 之前通过 {@link UserInfoHeaderStrippedRequestWrapper} 移除请求中的
 * {@code X-CurrentUserInfo} 头，使下游过滤器链（含内省令牌）与控制器无法读取到该头。
 * 认证完成后，{@code ApplicationUserContextFilter} 拦截器会依据内省结果重新设置真实用户信息。
 * </p>
 * <p>
 * <b>注：</b>当前 {@code doFilterInternal} 仅做请求头包装，并未主动清除
 * {@link ApplicationLinkContext#currentUserInfos} 线程本地变量。若上游 {@code ApplicationContextFilter}
 * 已根据外部请求头写入了用户信息，则需要确认该线程变量是否会在后续被覆盖或清理，否则存在身份残留风险。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
@Component
@Order(SecurityProperties.DEFAULT_FILTER_ORDER - 1)
public class StripUserInfoHeaderFilter extends OncePerRequestFilter {

    /**
     * 移除请求中的 {@code X-CurrentUserInfo} 头后继续过滤器链
     * <p>
     * 仅以 {@link UserInfoHeaderStrippedRequestWrapper} 包装请求继续过滤器链，
     * 使后续内省令牌与业务处理拿不到 {@code X-CurrentUserInfo} 头；
     * 本方法不主动清除 {@link ApplicationLinkContext#currentUserInfos} 线程本地变量。
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
        // 包装请求移除 X-CurrentUserInfo 头，使下游过滤器链与控制器无法读取
        filterChain.doFilter(new UserInfoHeaderStrippedRequestWrapper(request), response);
    }

    /**
     * 移除 {@code X-CurrentUserInfo} 请求头的包装器
     * <p>
     * HTTP 请求头大小写不敏感，因此重写 {@code getHeader}、{@code getHeaders}、
     * {@code getHeaderNames} 时均使用 {@code equalsIgnoreCase} 过滤目标头。
     * </p>
     *
     * @author maozi
     */
    private static class UserInfoHeaderStrippedRequestWrapper extends HttpServletRequestWrapper {

        /** 需要移除的请求头名称 */
        private static final String HEADER_TO_STRIP = ApplicationLinkContext.CURRENT_USER_INFO_KEY;

        /**
         * 构造方法
         *
         * @param request 原始请求
         */
        UserInfoHeaderStrippedRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        /**
         * 获取指定名称的请求头值
         *
         * @param name 请求头名称
         * @return 目标头返回 null，其余返回原始请求的头值
         */
        @Override
        public String getHeader(String name) {
            if (HEADER_TO_STRIP.equalsIgnoreCase(name)) {
                return null;
            }
            return super.getHeader(name);
        }

        /**
         * 获取指定名称的请求头值枚举
         *
         * @param name 请求头名称
         * @return 目标头返回空枚举，其余返回原始请求的头值枚举
         */
        @Override
        public Enumeration<String> getHeaders(String name) {
            if (HEADER_TO_STRIP.equalsIgnoreCase(name)) {
                return Collections.emptyEnumeration();
            }
            return super.getHeaders(name);
        }

        /**
         * 获取剔除目标头后的请求头名称枚举
         *
         * @return 不包含目标头的请求头名称枚举
         */
        @Override
        public Enumeration<String> getHeaderNames() {
            Set<String> names = new LinkedHashSet<>();
            Enumeration<String> original = super.getHeaderNames();
            while (original.hasMoreElements()) {
                String name = original.nextElement();
                if (!HEADER_TO_STRIP.equalsIgnoreCase(name)) {
                    names.add(name);
                }
            }
            return Collections.enumeration(names);
        }

    }

}
