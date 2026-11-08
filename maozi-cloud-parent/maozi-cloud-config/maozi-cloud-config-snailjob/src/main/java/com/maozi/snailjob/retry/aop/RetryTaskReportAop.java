package com.maozi.snailjob.retry.aop;

import cn.hutool.extra.spring.SpringUtil;
import com.aizuda.snailjob.client.core.RetryCondition;
import com.aizuda.snailjob.client.core.retryer.RetryTaskTemplateBuilder;
import com.aizuda.snailjob.client.core.retryer.RetryType;
import com.aizuda.snailjob.client.core.retryer.SnailJobTemplate;
import com.maozi.common.context.ApplicationLinkContext;
import com.maozi.snailjob.retry.annotation.RetryTask;
import com.maozi.snailjob.retry.context.RetryContextHolder;
import com.maozi.snailjob.retry.executor.RetryExecutorTask;
import com.maozi.snailjob.retry.param.RetryTaskParam;
import io.opentelemetry.api.trace.Span;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * 自定义重试上报切面
 * <p>
 * 拦截标注了 {@code @RetryTask} 注解的方法，处理方法执行失败（抛出异常或结果集判定需要重试）后的重试逻辑。
 * 非重试流量下按注解上报策略处理：{@link RetryType#ONLY_LOCAL} 与 {@link RetryType#LOCAL_REMOTE}
 * 策略先在本地按次数与间隔同步重试；{@link RetryType#ONLY_REMOTE} 与 {@link RetryType#LOCAL_REMOTE}
 * 策略在本地重试失败后将方法信息封装为 {@link RetryTaskParam} 上报 SnailJob 服务端，
 * 由统一重试处理器 {@link RetryExecutorTask} 在服务端调度时远程回调执行。
 * </p>
 * <p>
 * 重试流量（{@code RetryContextHolder} 已标记）下方法执行结果或异常直接放行，
 * 交由重试执行器根据执行情况判定本轮重试是否成功，避免重复本地重试与上报。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
@Aspect
@Component
public class RetryTaskReportAop {

    /** RetryTask 注解切点表达式 */
    private final String POINT = "@annotation(com.maozi.snailjob.retry.annotation.RetryTask)";

    /**
     * 环绕通知，处理 {@code @RetryTask} 方法失败后的本地重试与远程上报
     *
     * @param proceedingJoinPoint AOP 连接点
     * @return 正常执行无需重试时返回方法原结果，本地重试成功时返回重试结果，其余情况返回 {@code null}
     * @throws Throwable 方法执行异常在本地重试与上报处理后原样上抛
     */
    @Around(POINT)
    public Object around(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {

        MethodSignature signature = (MethodSignature) proceedingJoinPoint.getSignature();

        Method method = signature.getMethod();
        RetryTask retryTask = method.getAnnotation(RetryTask.class);

        Throwable ex = null;
        boolean retryTraffic = Boolean.TRUE.equals(RetryContextHolder.getRetry());

        try {

            Object result = proceedingJoinPoint.proceed();

            // 若为重试流量则直接返回数据，交给重试执行器处理
            if (retryTraffic) {
                return result;
            }

            // 结果集校验无需重试时直接返回结果，仍需重试时落入下方重试与上报流程
            if (retryCondition(retryTask, result)) {
                return result;
            }

        } catch (Throwable e) {
            ex = e;
        }

        // 只有抛异常 || (非重试流量 && 结果集校验方法需要重试)才会走到这里

        // 若为非重试流量则执行本地重试与远程上报
        if (!retryTraffic) {

            // 本地重试
            if (retryTask.retryStrategy() == RetryType.ONLY_LOCAL
                || retryTask.retryStrategy() == RetryType.LOCAL_REMOTE) {

                // 本地重试成功结束逻辑
                LocalRetryResultDTO localRetryResult =
                    localRetry(retryTask.localTimes(), retryTask.localInterval(), proceedingJoinPoint, retryTask);
                if (localRetryResult.success()) {
                    return localRetryResult.result();
                }

            }

            // 重试失败则落库上报
            if (retryTask.retryStrategy() == RetryType.ONLY_REMOTE
                || retryTask.retryStrategy() == RetryType.LOCAL_REMOTE) {

                RetryTaskParam retryTaskParam = new RetryTaskParam();
                retryTaskParam.setBeanName(getBeanName(proceedingJoinPoint.getTarget()));
                retryTaskParam.setMethodName(method.getName());
                retryTaskParam.setParamTypes(signature.getParameterTypes());
                retryTaskParam.setData(proceedingJoinPoint.getArgs());
                retryTaskParam.setVersion(ApplicationLinkContext.getVersionDefault(ApplicationLinkContext.getVersion()));
                retryTaskParam.setRetryIfResultClassName(retryTask.retryIfResult().getName());

                // OpenTelemetry 无有效追踪上下文时 traceId 为全 0 占位值，此时回退链路上下文中的 traceId
                String traceId = Span.current().getSpanContext().getTraceId();
                boolean notHasTraceId = ApplicationLinkContext.TRACE_ID_VALUE.equals(traceId);
                retryTaskParam.setTraceId(notHasTraceId ? ApplicationLinkContext.getTraceId() : traceId);

                reportRetryTask(retryTask.scene(), retryTaskParam);

            }

        }

        if (Objects.nonNull(ex)) {
            throw ex;
        }

        return null;

    }

    /**
     * 通过重试结果判断器校验方法执行结果是否无需重试
     * <p>
     * 未配置结果判断器（NoRetry）时直接视为无需重试，不再调用判断器 Bean。
     * </p>
     *
     * @param retryTask RetryTask 注解
     * @param result 方法执行结果集
     * @return true 表示无需重试，false 表示结果集仍需重试
     */
    private boolean retryCondition(RetryTask retryTask, Object result) {
        Class<? extends RetryCondition> retryCondition = retryTask.retryIfResult();
        return RetryCondition.NoRetry.class.getName().equals(retryCondition.getName())
                ||
                !SpringUtil.getBean(retryCondition).shouldRetry(result);
    }

    /**
     * 获取代理目标类对应的 Spring Bean 名称
     * <p>
     * 取目标类简单名并将首字母小写（与 Spring 默认 Bean 命名规则一致），
     * 供重试执行器按名称获取 Bean 动态调用。
     * </p>
     *
     * @param bean 代理目标对象
     * @return Spring Bean 名称
     */
    private String getBeanName(Object bean) {
        return bean.getClass().getSimpleName().toLowerCase().charAt(0) + bean.getClass().getSimpleName().substring(1);
    }

    /**
     * 重试任务上报
     * <p>
     * 以带版本号前缀的业务场景值（{@code RetryExecutorTask.buildScene}）与统一重试处理器构建上报模板，
     * 将方法信息上报 SnailJob 服务端，由服务端按重试配置调度 {@code RetryExecutorTask} 远程回调执行。
     * </p>
     *
     * @param scene 业务场景值
     * @param retryTaskParam 重试任务参数
     */
    private void reportRetryTask(String scene, RetryTaskParam retryTaskParam) {

        SnailJobTemplate snailJobTemplate =
            RetryTaskTemplateBuilder.newBuilder().withScene(RetryExecutorTask.buildScene(scene)).withExecutorMethod(RetryExecutorTask.class)
                .withParam(retryTaskParam).build();

        snailJobTemplate.executeRetry();

    }

    /**
     * 本地重试
     * <p>
     * 按重试次数与间隔时间（秒）循环重新执行目标方法：执行成功且结果集校验无需重试即视为重试成功结束；
     * 期间抛出的异常忽略后继续下一轮重试，直至次数耗尽返回失败结果。
     * </p>
     *
     * @param localTimes 本地重试次数
     * @param localInterval 本地重试间隔时间（秒）
     * @param proceedingJoinPoint AOP 连接点
     * @param retryTask RetryTask 注解
     * @return 本地重试结果
     */
    private LocalRetryResultDTO localRetry(int localTimes, int localInterval, ProceedingJoinPoint proceedingJoinPoint,
        RetryTask retryTask) {

        int currentLocalTimes = 0;

        do {

            try {

                // 重试间隙，防止重试过快跟上一次结果一样
                if (localInterval > 0) {
                    Thread.sleep(localInterval * 1000L);
                }

                Object result = proceedingJoinPoint.proceed();
                if (retryCondition(retryTask, result)) {
                    return new LocalRetryResultDTO(true, result);
                }

            } catch (Throwable ignored) {
            }

            ++currentLocalTimes;

        } while (currentLocalTimes < localTimes);

        return new LocalRetryResultDTO(false, null);

    }

    /**
     * 本地重试结果
     * <p>
     * {@code RetryTaskReportAop} 内部使用的本地重试结果承载对象，
     * 仅用于切面内传递重试成功状态与成功时的结果集，不参与网络传输。
     * </p>
     *
     * @param success 是否重试成功
     * @param result  重试成功的结果集
     * @author maozi
     */
     private record LocalRetryResultDTO(boolean success, Object result) {}

}
