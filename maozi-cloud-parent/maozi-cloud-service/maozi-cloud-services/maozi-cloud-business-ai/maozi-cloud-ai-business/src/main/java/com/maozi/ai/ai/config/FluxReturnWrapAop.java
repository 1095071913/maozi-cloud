package com.maozi.ai.ai.config;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * Flux 返回值包装切面
 * <p>
 * 拦截 api.impl 包及其子包下返回值类型为 Flux 的方法。切面优先于
 * 入口日志切面（AbstractRequestEntranceLogAop，Order 为
 * Ordered.HIGHEST_PRECEDENCE + 1）执行，处于最外层；
 * 内层日志切面会将异常兜底为统一错误结果（非 Flux 对象），
 * 本切面在最终返回前将非 Flux 的返回值统一用 Flux.just 包装，
 * 保证流式接口的返回值始终为 Flux，避免类型不匹配导致响应失败。
 * </p>
 *
 * @author pengjinlong
 * @date 2026/8/17
 */
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class FluxReturnWrapAop {

    /** 切点表达式：拦截 api.impl 包及其子包下返回值类型为 Flux 的方法 */
    private static final String POINT = "execution(public reactor.core.publisher.Flux com.maozi.ai.ai.api.impl..*(..))";

    /**
     * 环绕通知：执行目标方法后，若返回值不是 Flux 则用 Flux.just 包装。
     *
     * @param proceedingJoinPoint AOP 连接点
     * @return Flux 包装后的方法执行结果
     * @throws Throwable 目标方法执行失败时抛出
     */
    @Around(POINT)
    public Object doAround(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {

        // 执行目标业务方法（含内层入口日志切面的异常兜底处理）
        Object result = proceedingJoinPoint.proceed();

        // 返回值已经是 Flux 则原样返回
        if (result instanceof Flux) {
            return result;
        }

        // 返回值为空时返回空流，避免 Flux.just 包装空指针
        if (result == null) {
            return Flux.empty();
        }

        // 非 Flux 返回值（如日志切面兜底的统一错误结果）用 Flux.just 包装
        return Flux.just(result);

    }

}
