package com.maozi.job.config;

import com.maozi.common.EnvironmentUtil;
import com.maozi.common.LogUtil;
import com.maozi.common.ObjectUtil;
import com.maozi.common.constant.LogTag;
import com.maozi.common.context.AbstractRequestContextHolder;
import com.maozi.common.context.ApplicationEnvironmentContext;
import com.maozi.common.context.SqlLogContext;
import com.maozi.common.context.VersionContext;
import com.maozi.common.enums.EnvironmentType;
import com.maozi.common.enums.LogCommonType;
import com.xxl.job.core.context.XxlJobHelper;
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
 * 定时任务入口日志切面
 * <p>
 * 拦截标注了 {@code @XxlJob} 注解的定时任务方法，记录任务执行日志，
 * 包括任务参数、执行时间、SQL 日志和异常信息。自动设置版本号到链路上下文，
 * 执行完成后清理上下文。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Slf4j
@Aspect
@Component
@Order(value = Ordered.HIGHEST_PRECEDENCE + 1)
public class JobEntranceLogAop {

    /** XxlJob 注解切点表达式 */
    private final String POINT = "@annotation(com.xxl.job.core.handler.annotation.XxlJob)";

    /**
     * 环绕通知，记录定时任务执行日志
     * <p>
     * 任务抛出的 {@code Exception} 会被捕获并记录日志后原样上抛，
     * 交由 XXL-JOB 框架按任务失败处理。
     * </p>
     *
     * @param proceedingJoinPoint AOP 连接点
     * @throws Throwable 任务执行异常原样上抛
     */
    @Around(POINT)
    public void around(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {

        // 标记任务执行是否发生异常，用于最终日志级别判断
        boolean error = false;

        // 任务开始执行时间戳
        long startTime = System.currentTimeMillis();

        // 获取任务参数
        String arg = XxlJobHelper.getJobParam();

        // 将当前应用版本写入版本上下文，保证任务内发起的远程调用按版本灰度路由
        VersionContext.CONTEXT.set(ApplicationEnvironmentContext.VERSION);

        // 日志信息集合
        Map<String, String> logs = new LinkedHashMap<>(16);

        // 记录日志类型与任务方法
        logs.put(LogTag.TYPE, LogCommonType.JOB.getDesc());
        logs.put(LogTag.FUNCTION, proceedingJoinPoint.getSignature().getDeclaringTypeName()
            + ":" + proceedingJoinPoint.getSignature().getName());

        // 非生产环境直接记录任务参数；生产环境为避免参数外泄，仅在异常时补充记录
        boolean isNotProd = EnvironmentUtil.notEnvironment(EnvironmentType.PROD);
        if (isNotProd) {
            logs.put(LogTag.PARAM, arg);
        }

        try {
            // 执行任务方法
            proceedingJoinPoint.proceed();
        } catch (Exception e) {

            // 标记任务执行发生异常
            error = true;

            // 记录异常堆栈日志，便于排查问题
            LogUtil.error(log, e);

            // 生产环境异常时补充记录任务参数
            if (!isNotProd) {
                logs.put(LogTag.PARAM, arg);
            }

            // 记录异常描述
            logs.put(LogTag.ERROR_DESC, e.getLocalizedMessage());

            // 记录异常发生的第一行代码位置
            StackTraceElement[] errorLines = e.getStackTrace();
            if (errorLines.length > 0) {
                logs.put(LogTag.ERROR_LINE, errorLines[0].toString());
            }

            // 异常原样上抛，交由 XXL-JOB 框架按任务失败处理
            throw e;

        } finally {

            // 记录任务执行过程中的 SQL 日志
            StringBuilder sqlLog = SqlLogContext.CONTEXT.get();
            if (ObjectUtil.isNotNullEmpty(sqlLog)) {
                logs.put(LogTag.SQL, sqlLog.toString());
            }

            // 记录任务执行耗时
            logs.put(LogTag.RT, (System.currentTimeMillis() - startTime) + " ms");

            // 按执行结果以 info/error 级别输出日志
            LogUtil.log(log, error, logs);

            // 清理线程上下文，防止线程池复用导致数据泄漏
            AbstractRequestContextHolder.clearContext();

        }

    }

}
