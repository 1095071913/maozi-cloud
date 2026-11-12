package com.maozi.snailjob.job.config;

import com.aizuda.snailjob.model.dto.ExecuteResult;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * SnailJob 任务方法调用切面
 * <p>
 * 拦截标注了 {@code @JobExecutor} 注解的任务方法，按方法声明的返回类型统一包装执行结果：
 * 声明返回 {@code ExecuteResult} 或 {@code Object}（可承载 {@code ExecuteResult}）时
 * 原样返回方法结果，避免二次包装导致服务端状态判定错误；声明为其他返回类型（含 void）时
 * 包装为 {@code ExecuteResult.success} 返回，使任务结果符合 SnailJob 的返回值契约。
 * 方法抛出的异常不在此处理，原样上抛由 SnailJob 框架按任务失败处理。
 * </p>
 * <p>
 * 仅拦截方法级 {@code @JobExecutor} 注解，类级注解与 Map/Reduce 等
 * 其他注解的任务不经过本切面，仍按 SnailJob 原生契约执行。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Aspect
@Component
@Order(Integer.MIN_VALUE)
public class SnailJobInvokeAop {

    /** JobExecutor 注解切点表达式 */
    private final String POINT = "@annotation(com.aizuda.snailjob.client.job.core.annotation.JobExecutor)";

    /**
     * 环绕通知，按方法声明的返回类型包装任务执行结果
     * <p>
     * 声明返回 {@code ExecuteResult} 或 {@code Object} 时原样返回方法结果，
     * 其余返回类型（含 void）包装为 {@code ExecuteResult.success}。
     * </p>
     *
     * @param proceedingJoinPoint AOP 连接点
     * @return 方法原结果或包装后的 {@code ExecuteResult}
     * @throws Throwable 目标方法抛出的异常原样上抛
     */
    @Around(POINT)
    public Object around(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {

        MethodSignature signature = (MethodSignature) proceedingJoinPoint.getSignature();

        Object result = proceedingJoinPoint.proceed();

        return signature.getMethod().getReturnType().isAssignableFrom(ExecuteResult.class) ?
                result
                :
                ExecuteResult.success(result);

    }

}
