package com.maozi.snailjob.retry.executor;

import cn.hutool.extra.spring.SpringUtil;
import com.aizuda.snailjob.client.core.RetryCondition;
import com.aizuda.snailjob.client.core.annotation.ExecutorMethodRegister;
import com.aizuda.snailjob.client.core.strategy.ExecutorMethod;
import com.maozi.common.CollectionUtil;
import com.maozi.common.EnvironmentUtil;
import com.maozi.common.LogUtil;
import com.maozi.common.ObjectUtil;
import com.maozi.common.constant.LogTag;
import com.maozi.common.context.ApplicationEnvironmentContext;
import com.maozi.common.context.ApplicationLinkContext;
import com.maozi.common.enums.EnvironmentType;
import com.maozi.common.enums.LogCommonType;
import com.maozi.common.result.error.exception.BusinessResultException;
import com.maozi.snailjob.retry.context.RetryContextHolder;
import com.maozi.snailjob.retry.param.RetryTaskParam;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;

/**
 * 自定义统一重试处理器
 * <p>
 * SnailJob 服务端调度重试任务时的统一回调入口。依据上报时保存的
 * {@link RetryTaskParam} 方法信息，从 Spring 容器动态获取 Bean 反射回调目标方法，
 * 实现任意 {@code @RetryTask} 方法共用一个执行器场景上报与调度。
 * </p>
 * <p>
 * 回调前通过 {@code RetryContextHolder} 标记重试流量（目标方法上的
 * {@code RetryTaskReportAop} 据此放行执行结果，避免重复本地重试与上报），
 * 并恢复上报时保存的链路上下文（链路追踪 Id、版本号）；回调结束后统一清理，
 * 避免线程复用污染正常流量。
 * </p>
 *
 * @author maozi
 */
@Slf4j
@ExecutorMethodRegister(scene = RetryExecutorTask.SCENE)
public class RetryExecutorTask implements ExecutorMethod {

    /** 自定义统一重试处理器场景值 */
    public final static String SCENE = "retryExecutorTask";

    /**
     * 构建带版本号前缀的业务场景值（{@code ApplicationEnvironmentContext.VERSION + "_" + 场景值}）
     * <p>
     * 重试注册（{@code ExecutorMethodScanner} 扫描 {@code @RetryTask} 方法）与重试上报
     * （{@code RetryTaskReportAop} 构建上报模板）统一经本方法拼接场景值，
     * 保证注册键与上报键一致，同时按应用版本隔离不同版本服务间的重试流量。
     * </p>
     *
     * @param scene RetryTask 注解配置的业务场景值
     * @return 带版本号前缀的场景值
     */
    public static String buildScene(String scene) {
        return ApplicationEnvironmentContext.VERSION + "_" + scene;
    }

    /**
     * 执行远程重试回调
     * <p>
     * 标记当前为远程重试流量并恢复上报时保存的链路上下文后，
     * 按参数中的方法信息反射回调目标方法；执行结束（无论成败）统一清理线程上下文。
     * </p>
     *
     * @param params 回调参数数组，第一个元素为 {@link RetryTaskParam}
     * @return 固定返回 true（执行失败时直接抛出异常）
     */
    @SneakyThrows
    @Override
    public Object doExecute(Object params) {

        /* 请求开始时间戳 */
        long startTime = System.currentTimeMillis();

        Object[] args = (Object[]) params;
        RetryTaskParam retryParam = (RetryTaskParam) args[0];

        // 标记当前为远程重试流量,并恢复上报时保存的链路上下文(链路追踪Id、版本号)
        RetryContextHolder.setRetry(true);
        ApplicationLinkContext.setTraceId(retryParam.getTraceId());
        ApplicationLinkContext.setVersion(ApplicationLinkContext.getVersionDefault(retryParam.getVersion()));

        /* 日志信息集合 */
        Map<String, String> logs = CollectionUtil.newHashMap();
        logs.put(LogTag.TYPE, LogCommonType.RPC_RETRY.getDesc());
        logs.put(LogTag.FUNCTION, retryParam.getBeanName() + ":" + retryParam.getMethodName());
        // 非生产环境记录请求参数
        if (EnvironmentUtil.notEnvironment(EnvironmentType.PROD)) {
            logs.put(LogTag.PARAM, Arrays.toString(retryParam.getData()));
        }

        try {

            boolean result = executeTask(retryParam);
            if (!result) {
                throw new BusinessResultException("远程重试失败");
            }

        }catch (Exception e){

            Throwable ex = e.getCause();
            if(ObjectUtil.isNullEmpty(ex)){
                ex = e;
            }

            // 记录异常堆栈日志，便于排查问题
            LogUtil.error(log, ex);

            // 异常时补充记录请求参数（生产环境同样记录），并收集错误描述信息
            logs.put(LogTag.PARAM, Arrays.toString(retryParam.getData()));

            logs.put(LogTag.ERROR_DESC, ex.getLocalizedMessage());

            // 记录异常发生的第一行代码位置
            StackTraceElement[] errorLines = ex.getStackTrace();
            if (errorLines.length > 0) {
                logs.put(LogTag.ERROR_LINE, errorLines[0].toString());
            }

            throw ex;

        }finally {

            // 记录本次请求过程中执行的 SQL 日志（从 ThreadLocal 中获取）
            StringBuilder sqlLog = LogUtil.sqlLog.get();
            if (ObjectUtil.isNotNullEmpty(sqlLog)) {
                logs.put(LogTag.SQL, sqlLog.toString());
            }

            // 记录响应时间
            logs.put(LogTag.RT, (System.currentTimeMillis() - startTime) + " ms");

            LogUtil.log(log,logs.containsKey(LogTag.ERROR_DESC), logs);

            // 清理重试流量标记与链路上下文
            RetryContextHolder.clear();
            ApplicationLinkContext.clearContext();
        }

        return true;

    }

    /**
     * 动态调用重试方法
     * <p>
     * 按参数中的 Bean 名称从 Spring 容器获取代理对象反射调用目标方法
     * （经过代理保证 AOP 切面生效），再按重试结果判断器校验结果集：
     * 判定无需重试视为执行成功，判定仍需重试视为执行失败（交由服务端下轮调度）。
     * 方法抛出异常时异常原样上抛，同样由服务端判定本轮重试失败。
     * </p>
     *
     * @param task 重试任务参数
     * @return true 表示重试方法执行成功 false 表示结果集仍需重试
     * @throws Exception 反射调用或获取判断器失败时抛出
     */
    private boolean executeTask(RetryTaskParam task) throws Exception {

        Object bean = SpringUtil.getBean(task.getBeanName());

        Method method = bean.getClass().getMethod(task.getMethodName(), task.getParamTypes());

        Object result = method.invoke(bean, task.getData());
        if (!RetryCondition.NoRetry.class.getName().equals(task.getRetryIfResultClassName())) {

            RetryCondition retryCondition =
                (RetryCondition) SpringUtil.getBean(Class.forName(task.getRetryIfResultClassName()));

            return !retryCondition.shouldRetry(result);

        }

        return true;

    }

}
