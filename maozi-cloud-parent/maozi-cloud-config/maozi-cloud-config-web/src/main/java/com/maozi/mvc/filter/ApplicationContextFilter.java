package com.maozi.mvc.filter;

import com.maozi.common.ObjectUtil;
import com.maozi.common.context.ApplicationLinkContext;
import com.maozi.common.dto.CurrentUserInfo;
import io.opentelemetry.api.trace.Span;
import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 应用链路上下文过滤器（版本号、当前用户信息、traceId）
 * <p>
 * 以 Servlet Filter 形式注册，优先级设为最高（{@link Ordered#HIGHEST_PRECEDENCE}），
 * 确保在 Spring Security 过滤器链（包括 {@code BearerTokenAuthenticationFilter}、
 * {@code OpaqueTokenIntrospector} 以及 {@code /oauth/**} 端点）之前执行。
 * </p>
 * <p>
 * 负责在请求进入时初始化链路上下文：
 * <ul>
 *   <li>从请求头提取版本号（空时取默认版本）写入 {@link ApplicationLinkContext#versions}，
 *       使版本号在 {@code OpaqueTokenIntrospector} 内省令牌和 {@code /oauth/**} 端点处理时即可读取，
 *       用于灰度路由等场景</li>
 *   <li>上下文中尚无当前用户信息时，尝试从请求头解析并写入（供网关/上游透传用户信息的场景）</li>
 *   <li>当前链路尚未产生有效 traceId 时，从请求头读取（为空则生成随机 UUID），
 *       写入上下文并回写到响应头</li>
 * </ul>
 * 请求完成后自动清理上下文，防止线程池复用导致的数据泄漏。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApplicationContextFilter extends OncePerRequestFilter {

    /**
     * 请求处理前初始化链路上下文（版本号、当前用户信息、traceId）
     * <p>
     * 从请求头获取版本号（空时取默认版本）存入线程本地变量；
     * 上下文中尚无用户信息时尝试从请求头解析补齐；
     * 当前链路无有效 traceId 时从请求头读取（为空则生成随机 UUID）并回写响应头。
     * 该过滤器在 Spring Security 之前执行，确保版本号在
     * {@code OpaqueTokenIntrospector} 和 {@code /oauth/**} 端点中可用。
     * </p>
     *
     * @param request HTTP 请求
     * @param response HTTP 响应
     * @param filterChain 过滤器链
     * @throws ServletException Servlet 异常
     * @throws IOException IO 异常
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, @Nonnull HttpServletResponse response, @Nonnull FilterChain filterChain) throws ServletException, IOException {

        // 从请求头提取版本号（为空时取默认版本），写入链路上下文，供灰度路由等场景使用
        String version = ApplicationLinkContext.getVersionDefault(request.getHeader(ApplicationLinkContext.VERSION_KEY));
        ApplicationLinkContext.setVersion(version);

        // 上下文中尚无当前用户信息时，从请求头解析并补齐（供网关/上游透传用户信息的场景）
        CurrentUserInfo currentUserInfo = ApplicationLinkContext.getCurrentUserInfo();
        if(ObjectUtil.isNullEmpty(currentUserInfo)){
            ApplicationLinkContext.setCurrentUserInfo(request.getHeader(ApplicationLinkContext.CURRENT_USER_INFO_KEY));
        }

        // 当前链路尚未产生有效 traceId（OTel Span 为 32 个 0 的占位值）时，从请求头读取，为空则生成随机 UUID，写入上下文并回写响应头
        String traceId = Span.current().getSpanContext().getTraceId();
        boolean notHasTraceId = ApplicationLinkContext.TRACE_ID_VALUE.equals(traceId);
        if(notHasTraceId){
            traceId = request.getHeader(ApplicationLinkContext.TRACE_ID_KEY);
            if(ObjectUtil.isNullEmpty(traceId)){
                traceId = UUID.randomUUID().toString();
            }
            ApplicationLinkContext.setTraceId(traceId);
            response.setHeader(ApplicationLinkContext.TRACE_ID_KEY,ApplicationLinkContext.getTraceId());
        }

        try {
            // 继续执行过滤器链（包括 Spring Security 认证和后续业务处理）
            filterChain.doFilter(request, response);
        } finally {
            // 清理链路上下文，防止线程池复用时数据泄漏
            ApplicationLinkContext.clearContext();
        }

    }

}
