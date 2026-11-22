package com.maozi.oauth.config;

import com.maozi.common.context.PassThroughExceptionContainer;
import jakarta.annotation.PostConstruct;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Spring Security 权限不足异常处理器
 * <p>
 * 当 Spring Security 判定当前用户无权访问时抛出 {@link AccessDeniedException}
 * （如方法级 {@code @RolesAllowed} 校验失败）。启动时将其注册为透传异常，
 * 请求入口日志切面捕获后原样上抛；MVC 层捕获后同样直接重新抛出，
 * 最终交由 Spring Security 的 {@link AccessDeniedHandler} 统一处理。
 * </p>
 * <p>
 * 必须设置为最高优先级：Spring MVC 按 Advice 顺序选取第一个含匹配处理方法的 Advice，
 * 而非跨 Advice 选取最具体的异常类型。若先匹配到 config-web 模块全局兜底处理器
 * （{@code RestErrorHandler}）的 {@code Exception.class} 处理方法，
 * 权限不足异常将被其吞并为系统错误，无法到达 Spring Security 的异常处理机制。
 * </p>
 *
 * @author maozi
 * @date 2026/11/22
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class SecurityErrorHandler {

    /**
     * 注册权限不足异常为透传异常
     * <p>
     * 向 {@link PassThroughExceptionContainer} 注册 {@link AccessDeniedException}，
     * 请求入口日志切面捕获到该异常（含子类）时原样上抛，不封装为统一错误结果。
     * 容器为全局静态集合，在应用启动阶段（Bean 初始化时）写入，早于任何请求处理。
     * </p>
     */
    @PostConstruct
    private void registerPassThroughException() {
        PassThroughExceptionContainer.add(AccessDeniedException.class);
    }

    /**
     * 处理权限不足异常
     * <p>
     * 此处不做处理，直接重新抛出，交由 Spring Security 的异常处理机制统一处理
     * （由 {@link AccessDeniedHandler} 写入权限不足的统一错误响应）。
     * </p>
     *
     * @param e AccessDeniedException 权限不足异常
     * @return 不会返回，直接抛出异常
     * @throws AccessDeniedException 原样上抛，交由 Spring Security 异常处理机制统一处理
     */
    @ExceptionHandler(AccessDeniedException.class)
    public Object handleAccessDeniedException(AccessDeniedException e) {
        throw e;
    }

}
