package com.maozi.ai.ai.config;

import com.maozi.common.CollectionUtil;
import com.maozi.common.context.CurrentUserInfoContext;
import com.maozi.common.dto.CurrentUserInfoDTO;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * AI 工具调用权限校验切面
 * <p>
 * 拦截标注 {@link Tool} 注解的 AI 函数调用方法：若同时标注 {@link PreAuthorize}，
 * 先基于链路上下文当前用户补齐 SecurityContext 认证信息交由内层原生拦截器
 * 完成权限校验，未标注则直接执行。背景：SSE 流式对话中工具方法运行在
 * Reactor 线程，SecurityContext 不随线程传递导致原生拦截器校验失败，
 * 故本切面先于其执行并覆盖写入认证信息，执行完毕清空防止残留到线程池线程。
 * </p>
 *
 * @author pengjinlong
 * @date 2026/8/30
 */
@Aspect
@Component
@Order(FunctionCallSecurityAop.ORDER_BEFORE_PRE_AUTHORIZE)
public class FunctionCallSecurityAop {

    /**
     * 切面顺序：须先于原生 {@code @PreAuthorize} 方法拦截器
     * （{@code AuthorizationInterceptorsOrder.PRE_AUTHORIZE}，顺序 200）执行
     */
    public static final int ORDER_BEFORE_PRE_AUTHORIZE = 199;

    /**
     * 环绕通知：为标注 {@code @PreAuthorize} 的 AI 工具方法临时补齐当前用户认证信息
     * 后执行目标方法，
     * 权限表达式校验由内层原生 {@code @PreAuthorize} 拦截器完成
     *
     * @param joinPoint AOP 连接点
     * @return 目标方法执行结果
     * @throws Throwable 未登录抛出权限不足业务异常、权限校验不通过抛出权限不足异常，
     * 或目标方法抛出的异常
     */
    @Around("@annotation(org.springframework.ai.tool.annotation.Tool)")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {

        // 获取目标方法
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();

        // 获取方法上的 @PreAuthorize 注解（AnnotationUtils 会沿父类、
        // 接口方法及元注解查找），未标注则无需校验直接执行
        PreAuthorize preAuthorize = AnnotationUtils.findAnnotation(method, PreAuthorize.class);
        if (preAuthorize == null) {
            return joinPoint.proceed();
        }

        try {
            // 临时写入基于链路上下文当前用户构建的认证信息，
            // 供内层原生 @PreAuthorize 拦截器完成权限校验
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
     * 从 {@link CurrentUserInfoContext}（TransmittableThreadLocal，可跨线程池传递）
     * 获取当前登录用户，
     * 直接从用户信息中取其权限标识列表（不发起 RPC 查询），
     * 构建携带权限的已认证令牌。
     * </p>
     *
     * @return 携带当前用户权限标识的认证信息
     * @throws BusinessResultException 链路上下文中不存在登录用户（未登录）时抛出
     */
    private Authentication buildAuthentication() {

        // 获取链路上下文中的当前登录用户，未登录时抛出权限不足异常
        // （收紧校验，不默认放行）
        CurrentUserInfoDTO currentUserInfo = CurrentUserInfoContext.CONTEXT.get();
        if (Objects.isNull(currentUserInfo)) {
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
