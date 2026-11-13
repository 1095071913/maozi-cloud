package com.maozi.mvc.filter;

import com.maozi.common.ObjectUtil;
import com.maozi.common.context.AbstractRequestContextHolder;
import com.maozi.common.context.CurrentUserInfoContext;
import com.maozi.common.context.TestFlowContext;
import com.maozi.common.context.TraceIdContext;
import com.maozi.common.context.VersionContext;
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
 * 应用链路上下文过滤器（版本号、测试流量标记、当前用户信息、traceId）
 * <p>
 * 以 Servlet Filter 形式注册，优先级设为最高（{@link Ordered#HIGHEST_PRECEDENCE}），
 * 确保在 Spring Security 过滤器链（包括 {@code BearerTokenAuthenticationFilter}、
 * {@code OpaqueTokenIntrospector} 以及 {@code /oauth/**} 端点）之前执行。
 * </p>
 * <p>
 * 请求进入时从请求头初始化版本号、测试流量标记、当前用户信息和 traceId 上下文
 * （各字段的提取条件见 {@link #doFilterInternal} 内注释），
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
     * 请求处理前初始化链路上下文（版本号、测试流量标记、当前用户信息、traceId），请求结束后清理上下文
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
        VersionContext.CONTEXT.set(request.getHeader(VersionContext.KEY));

        // 从请求头提取测试流量标记（"true" 表示测试流量），写入链路上下文
        TestFlowContext.CONTEXT.set(request.getHeader(TestFlowContext.KEY));

        // 上下文中尚无当前用户信息时，从请求头解析并补齐（供网关/上游透传用户信息的场景）
        CurrentUserInfo currentUserInfo = CurrentUserInfoContext.CONTEXT.get();
        if(ObjectUtil.isNullEmpty(currentUserInfo)){
            CurrentUserInfoContext.CONTEXT.set(request.getHeader(CurrentUserInfoContext.KEY));
        }

        // OTel 尚未产生有效 traceId（32 个 0 占位值）时，从请求头读取，为空则生成随机 UUID，写入上下文并回写响应头
        String traceId = Span.current().getSpanContext().getTraceId();
        if(TraceIdContext.TRACE_ID_VALUE.equals(traceId)){
            traceId = request.getHeader(TraceIdContext.KEY);
            if(ObjectUtil.isNullEmpty(traceId)){
                traceId = UUID.randomUUID().toString();
            }
            TraceIdContext.CONTEXT.set(traceId);
            response.setHeader(TraceIdContext.KEY, traceId);
        }


        try {
            // 继续执行过滤器链（包括 Spring Security 认证和后续业务处理）
            filterChain.doFilter(request, response);
        } finally {
            // 清理链路上下文，防止线程池复用时数据泄漏
            AbstractRequestContextHolder.clearContext();
        }

    }

}
