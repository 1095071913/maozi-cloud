package com.maozi.snailjob.job.config;

import com.aizuda.snailjob.common.core.model.JobContext;
import com.aizuda.snailjob.common.log.SnailJobLog;
import com.maozi.common.EnvironmentUtil;
import com.maozi.common.LogUtil;
import com.maozi.common.ObjectUtil;
import com.maozi.common.constant.LogTag;
import com.maozi.common.context.ApplicationEnvironmentContext;
import com.maozi.common.context.ApplicationLinkContext;
import com.maozi.common.enums.EnvironmentType;
import com.maozi.common.enums.LogCommonType;
import com.maozi.snailjob.job.config.context.SnailJobParamContext;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SnailJob 定时任务入口日志切面
 * <p>
 * 拦截 SnailJob 注解执行器的任务入口方法 {@code AbstractJobExecutor#jobExecute(JobContext)}，
 * 该方法为 {@code AnnotationJobExecutor}、{@code AnnotationMapJobExecutor}、
 * {@code AnnotationMapReduceJobExecutor} 等注解执行器共用的入口，
 * 由 {@code JobEndPoint} 通过 Spring 容器获取执行器 Bean 后调用，故经过代理可被拦截，
 * 一次拦截覆盖普通、分片、Map、Reduce、MergeReduce 全部任务类型。
 * 记录任务执行器信息、任务参数与调度耗时。
 * </p>
 * <p>
 * 注意：任务体由 SnailJob 提交至线程池异步执行，本切面记录的是调度入口日志，
 * RT 为派发耗时（非任务执行耗时），任务执行结果与异常由 SnailJob 客户端上报服务端；
 * 派发阶段的异常记录日志后继续上抛。不直接拦截 {@code @JobExecutor} 注解方法的原因：
 * 注解方法由执行器反射调用，且 Map/Reduce 等任务类型使用独立注解，逐注解拦截易遗漏。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Slf4j
@Aspect
@Component
@Order(value = Ordered.HIGHEST_PRECEDENCE + 1)
public class SnailJobEntranceLogAop {

    /**
     * SnailJob 注解执行器任务入口切点表达式
     * （入口方法声明于父类 AbstractJobExecutor，拦截全部注解执行器子类）
     */
    private final String POINT =
        "execution(* com.aizuda.snailjob.client.job.core.executor.AbstractJobExecutor.jobExecute(..))";

    /**
     * 环绕通知，记录定时任务调度入口日志
     * <p>
     * 派发阶段的异常记录日志后继续上抛，由 SnailJob 框架按任务失败处理。
     * </p>
     *
     * @param proceedingJoinPoint AOP 连接点
     * @return 入口方法原返回值（jobExecute 为 void，恒为 null）
     * @throws Throwable 派发阶段抛出的异常原样上抛
     */
    @Around(POINT)
    public Object around(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {

        boolean error = false;

        long startTime = System.currentTimeMillis();

        Object[] args = proceedingJoinPoint.getArgs();
        JobContext jobContext = (JobContext) args[0];
        String businessParam = (String) jobContext.getJobArgsHolder().getJobParams();
        SnailJobParamContext.setJobParams(businessParam);

        ApplicationLinkContext.setVersion(ApplicationEnvironmentContext.VERSION);

        Map<String, String> logs = new LinkedHashMap<>(16);

        logs.put(LogTag.TYPE, LogCommonType.JOB.getDesc());
        logs.put(LogTag.FUNCTION, getExecutorInfo(proceedingJoinPoint));

        // 非生产环境直接记录任务参数；生产环境为避免参数外泄，仅在异常时补充记录
        boolean isNotProd = EnvironmentUtil.notEnvironment(EnvironmentType.PROD);
        if (isNotProd) {
            logs.put(LogTag.PARAM, businessParam);
        }

        try {

            return proceedingJoinPoint.proceed(args);

        } catch (Exception e) {

            error = true;

            LogUtil.error(log, e);

            if (!isNotProd) {
                logs.put(LogTag.PARAM, businessParam);
            }

            logs.put(LogTag.ERROR_DESC, e.getLocalizedMessage());

            StackTraceElement[] errorLines = e.getStackTrace();
            if (errorLines.length > 0) {
                logs.put(LogTag.ERROR_LINE, errorLines[0].toString());
            }

            throw e;

        } finally {

            StringBuilder sqlLog = LogUtil.SQL_LOG.get();
            if (ObjectUtil.isNotNullEmpty(sqlLog)) {
                logs.put(LogTag.SQL, sqlLog.toString());
            }

            logs.put(LogTag.RT, (System.currentTimeMillis() - startTime) + " ms");

            String log = LogUtil.convertLog(logs);
            if (error) {
                SnailJobLog.REMOTE.error(log);
            } else {
                SnailJobLog.REMOTE.info(log);
            }

            ApplicationLinkContext.clearContext();
            SnailJobParamContext.clearJobParams();

        }

    }

    /**
     * 获取任务执行器信息
     * <p>
     * 取连接点方法入参 {@code JobContext} 的 {@code executorInfo}
     * （格式为 {@code beanName.methodName}，即服务端配置的执行器）；
     * 无法获取时回退为连接点方法签名。
     * </p>
     *
     * @param proceedingJoinPoint AOP 连接点
     * @return 任务执行器信息
     */
    private String getExecutorInfo(ProceedingJoinPoint proceedingJoinPoint) {

        for (Object param : proceedingJoinPoint.getArgs()) {
            if (param instanceof JobContext jobContext && ObjectUtil.isNotNullEmpty(jobContext.getExecutorInfo())) {
                return jobContext.getExecutorInfo();
            }
        }

        return proceedingJoinPoint.getSignature().getDeclaringTypeName()
            + ":" + proceedingJoinPoint.getSignature().getName();

    }

}
