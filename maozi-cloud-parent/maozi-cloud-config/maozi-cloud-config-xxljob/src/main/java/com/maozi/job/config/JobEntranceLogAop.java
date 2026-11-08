package com.maozi.job.config;

import com.maozi.common.EnvironmentUtil;
import com.maozi.common.LogUtil;
import com.maozi.common.ObjectUtil;
import com.maozi.common.constant.LogTag;
import com.maozi.common.context.ApplicationEnvironmentContext;
import com.maozi.common.context.ApplicationLinkContext;
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
@Order(value = Ordered.HIGHEST_PRECEDENCE + 1 )
public class JobEntranceLogAop {

    /** XxlJob 注解切点表达式 */
    private final String POINT = "@annotation(com.xxl.job.core.handler.annotation.XxlJob)";

    /**
     * 环绕通知，记录定时任务执行日志
     * <p>
     * 任务抛出的 {@code Exception} 会被捕获并记录日志，不再向上抛出，
     * 仅 {@code Error} 级别的严重错误会继续向上传播。
     * </p>
     *
     * @param proceedingJoinPoint AOP 连接点
     * @throws Throwable 仅 Error 级别严重错误向上抛出（Exception 已被捕获记录）
     */
    @Around(POINT)
    public void around(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {

        boolean error = false;

        long startTime = System.currentTimeMillis();

        String arg = XxlJobHelper.getJobParam();

        ApplicationLinkContext.setVersion(ApplicationEnvironmentContext.VERSION);

        Map<String, String> logs = new LinkedHashMap<>();

        logs.put(LogTag.TYPE, LogCommonType.JOB.getDesc());
        logs.put(LogTag.FUNCTION, proceedingJoinPoint.getSignature().getDeclaringTypeName() + ":" + proceedingJoinPoint.getSignature().getName());

        // 非生产环境直接记录任务参数；生产环境为避免参数外泄，仅在异常时补充记录
        Boolean isNotProd = EnvironmentUtil.notEnvironment(EnvironmentType.PROD);
        if(isNotProd){
            logs.put(LogTag.PARAM, arg);
        }

        try {proceedingJoinPoint.proceed();}catch (Exception e){

            error = true;

            LogUtil.error(log,e);

            if(!isNotProd){
                logs.put(LogTag.PARAM, arg);
            }

            logs.put(LogTag.ERROR_DESC, e.getLocalizedMessage());

            StackTraceElement[] errorLines = e.getStackTrace();
            if(errorLines.length > 0) {
                logs.put(LogTag.ERROR_LINE, errorLines[0].toString());
            }

        }finally {

            StringBuilder sqlLog = LogUtil.sqlLog.get();
            if (ObjectUtil.isNotNullEmpty(sqlLog)) {
                logs.put(LogTag.SQL, sqlLog.toString());
            }

            logs.put(LogTag.RT, (System.currentTimeMillis() - startTime) + " ms");

            LogUtil.log(log, error, logs);

            ApplicationLinkContext.clearContext();

        }

    }

}
