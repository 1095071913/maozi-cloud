package com.maozi.snailjob.job.config;

import com.aizuda.snailjob.common.core.model.JobContext;
import com.aizuda.snailjob.common.log.SnailJobLog;
import com.maozi.common.EnvironmentUtil;
import com.maozi.common.LogUtil;
import com.maozi.common.ObjectUtil;
import com.maozi.common.constant.LogTag;
import com.maozi.common.constant.SymbolConstant;
import com.maozi.common.context.AbstractRequestContextHolder;
import com.maozi.common.context.ApplicationEnvironmentContext;
import com.maozi.common.context.SqlLogContext;
import com.maozi.common.context.VersionContext;
import com.maozi.common.enums.EnvironmentTypeEnum;
import com.maozi.common.enums.LogCommonTypeEnum;
import com.maozi.snailjob.job.config.context.SnailJobParamContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
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
    private static final String POINT =
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

        // 入口方法入参
        Object[] args = proceedingJoinPoint.getArgs();

        // 提取任务业务参数，供任务执行过程读取
        JobContext jobContext = (JobContext) args[0];
        String businessParam = (String) jobContext.getJobArgsHolder().getJobParams();
        
        // 日志信息集合
        Map<String, String> logs = new LinkedHashMap<>(16);

        Throwable ex = null;
        try {

            // 写入任务参数上下文，供任务执行过程读取；将当前应用版本写入版本上下文，
            // 保证任务内发起的远程调用按版本灰度路由；set 位于 try 内，
            // 保证 set 之后、任务执行之前抛出异常时 finally 仍能清理线程上下文
            SnailJobParamContext.setJobParams(businessParam);
            VersionContext.CONTEXT.set(ApplicationEnvironmentContext.version);

            // 记录日志类型与任务执行器信息
            logs.put(LogTag.TYPE, LogCommonTypeEnum.JOB.getDesc());
            logs.put(LogTag.FUNCTION, getExecutorInfo(proceedingJoinPoint));

            Object result = proceedingJoinPoint.proceed(args);

            // 非生产环境记录任务参数
            if (EnvironmentUtil.notEnvironment(EnvironmentTypeEnum.PROD)) {
                logs.put(LogTag.PARAM, businessParam);
            }

            return result;

        } catch (Exception e) {

            ex = e;

            logs.put(LogTag.PARAM, businessParam);

            // 异常原样上抛，由 SnailJob 框架按任务失败处理
            throw e;

        } finally {

            // 记录当前线程累积的 SQL 日志
            // （任务体在独立线程池执行，其 SQL 日志不在本线程）
            StringBuilder sqlLog = SqlLogContext.CONTEXT.get();
            if (ObjectUtil.isNotNull(sqlLog)) {
                logs.put(LogTag.SQL, sqlLog.toString());
            }

            // 按执行结果输出远程日志（上报至 SnailJob 服务端）
            String log = LogUtil.convertLog(logs);
            if (ObjectUtil.isNotNull(ex)) {
                SnailJobLog.REMOTE.error(log, ex);
            } else {
                SnailJobLog.REMOTE.info(log);
            }

            // 清理线程上下文与任务参数，防止线程池复用导致数据泄漏
            SnailJobParamContext.clearJobParams();
            AbstractRequestContextHolder.clearContext();

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
            if (param instanceof JobContext jobContext) {

                String executorInfo = jobContext.getExecutorInfo();
                if (StringUtils.isNotEmpty(executorInfo)) {
                    return executorInfo;
                }
            }
        }

        Signature signature = proceedingJoinPoint.getSignature();
        return signature.getDeclaringTypeName()
            + SymbolConstant.COLON + signature.getName();

    }

}
