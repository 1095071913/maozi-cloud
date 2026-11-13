package com.maozi.ai.ai.config;

import com.maozi.common.CollectionUtil;
import com.maozi.common.ObjectUtil;
import com.maozi.common.context.CurrentUserInfoContext;
import com.maozi.common.dto.CurrentUserInfo;
import com.maozi.common.result.error.code.SystemErrorCode;
import com.maozi.common.result.error.exception.BusinessResultException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.method.AuthorizationInterceptorsOrder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI 工具调用权限校验切面
 * <p>
 * 拦截所有标注 {@link Tool} 注解的 AI 函数调用方法，若方法同时标注了
 * {@link PreAuthorize} 注解，则在执行前基于链路上下文当前用户补齐 SecurityContext 认证信息，
 * 交由内层原生拦截器完成权限校验，未标注则不校验直接执行。
 * </p>
 * <p>
 * 背景：AI 流式对话（SSE）中工具方法在 Reactor 线程上执行，SecurityContext
 * 不会随线程传递，导致原生 {@code @PreAuthorize} 方法级拦截器因获取不到认证信息而失败。
 * 本切面先于原生拦截器（Order 小于 {@link AuthorizationInterceptorsOrder#PRE_AUTHORIZE}）执行：
 * <ul>
 *   <li>基于链路上下文（TransmittableThreadLocal，可跨线程池传递）获取当前登录用户，
 *       直接从用户信息中取权限标识列表构建认证信息，不发起用户服务 RPC 查询</li>
 *   <li>无论当前线程是否已有认证信息，均将构建的认证信息临时写入 SecurityContext 后放行，
 *       保证流式线程与普通请求线程行为一致</li>
 *   <li>本切面不自行求值权限表达式，实际校验由内层原生 {@code @PreAuthorize} 拦截器完成
 *       （支持 hasAuthority、hasAnyAuthority、hasRole 等标准写法），使其在流式线程上也能正常工作，
 *       校验不通过抛出权限不足异常</li>
 *   <li>执行完毕清空 SecurityContext，避免认证信息残留到线程池线程</li>
 * </ul>
 * </p>
 *
 * @author pengjinlong
 * @since 2026/8/30 15:30
 */
@Aspect
@Component
@Order(199)
public class FunctionCallSecurityAop {

    /**
     * 环绕通知：为标注 {@code @PreAuthorize} 的 AI 工具方法临时补齐当前用户认证信息后执行目标方法，
     * 权限表达式校验由内层原生 {@code @PreAuthorize} 拦截器完成
     *
     * @param joinPoint AOP 连接点
     * @return 目标方法执行结果
     * @throws Throwable 未登录抛出权限不足业务异常、权限校验不通过抛出权限不足异常，或目标方法抛出的异常
     */
    @Around("@annotation(org.springframework.ai.tool.annotation.Tool)")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {

        // 获取目标方法
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();

        // 获取方法上的 @PreAuthorize 注解（AnnotationUtils 支持代理对象与注解合并查找），未标注则无需校验直接执行
        PreAuthorize preAuthorize = AnnotationUtils.findAnnotation(method, PreAuthorize.class);
        if (preAuthorize == null) {
            return joinPoint.proceed();
        }

        try {
            // 临时写入基于链路上下文当前用户构建的认证信息，供内层原生 @PreAuthorize 拦截器完成权限校验
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(buildAuthentication());
            SecurityContextHolder.setContext(securityContext);
            return joinPoint.proceed();
        } finally {
            // 执行完毕清空 SecurityContext，避免认证信息残留到线程池线程
            SecurityContextHolder.clearContext();
        }

    }

    /**
     * 基于链路上下文当前用户构建认证信息
     * <p>
     * 从 {@link CurrentUserInfoContext}（TransmittableThreadLocal，可跨线程池传递）获取当前登录用户，
     * 直接从用户信息中取其权限标识列表（不发起 RPC 查询），构建携带权限的已认证令牌。
     * </p>
     *
     * @return 携带当前用户权限标识的认证信息
     * @throws BusinessResultException 链路上下文中不存在登录用户（未登录）时抛出
     */
    private Authentication buildAuthentication() {

        // 获取链路上下文中的当前登录用户，未登录时按认证失败处理（收紧校验，不默认放行）
        CurrentUserInfo currentUserInfo = CurrentUserInfoContext.CONTEXT.get();
        if (ObjectUtil.isNullEmpty(currentUserInfo)) {
            throw new BusinessResultException(SystemErrorCode.PERMISSION_ERROR);
        }

        // 权限标识列表转换为 Spring Security 权限对象集合
        List<String> permissions = currentUserInfo.getPermissions();
        List<GrantedAuthority> authorities = CollectionUtil.isEmpty(permissions)
                ? CollectionUtil.newArrayList()
                : permissions.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());

        // 构建已认证令牌：主体为用户名，凭证为空，携带权限集合
        return new UsernamePasswordAuthenticationToken(currentUserInfo.getUsername(), null, authorities);

    }

}
