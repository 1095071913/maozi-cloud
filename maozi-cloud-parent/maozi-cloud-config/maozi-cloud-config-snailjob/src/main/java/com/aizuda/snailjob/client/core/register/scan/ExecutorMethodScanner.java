package com.aizuda.snailjob.client.core.register.scan;

import com.aizuda.snailjob.client.core.IdempotentIdGenerate;
import com.aizuda.snailjob.client.core.RetryCondition;
import com.aizuda.snailjob.client.core.Scanner;
import com.aizuda.snailjob.client.core.annotation.ExecutorMethodRegister;
import com.aizuda.snailjob.client.core.callback.complete.RetryCompleteCallback;
import com.aizuda.snailjob.client.core.retryer.RetryType;
import com.aizuda.snailjob.client.core.retryer.RetryerInfo;
import com.aizuda.snailjob.client.core.strategy.ExecutorMethod;
import com.aizuda.snailjob.common.log.SnailJobLog;
import com.maozi.snailjob.retry.annotation.RetryTask;
import com.maozi.snailjob.retry.executor.RetryExecutorTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 重试执行器扫描器
 * <p>
 * 启动时扫描 Spring 容器中带 {@code @ExecutorMethodRegister} 注解的 Bean，
 * 解析其执行器信息并注册为 SnailJob 重试执行器。
 * 在原生框架逻辑基础上扩展：当扫描到自定义统一重试处理器
 * {@code RetryExecutorTask} 时，会进一步扫描容器中所有带 {@code @RetryTask}
 * 注解的方法，为每个方法克隆一份绑定该统一处理器的注册信息，
 * 实现业务方法零配置接入远程重试。
 * </p>
 *
 * @author: opensnail
 * @date : 2023-05-10 11:10
 */
@Component
@Slf4j
public class ExecutorMethodScanner implements Scanner, ApplicationContextAware {

    /** Spring 应用上下文，用于扫描时获取容器中的 Bean */
    public ApplicationContext applicationContext;

    /**
     * 扫描容器中的重试执行器并构建注册信息
     * <p>
     * 遍历容器中所有 Bean，解析带 {@code @ExecutorMethodRegister} 注解的执行器：
     * 若为自定义统一重试处理器 {@code RetryExecutorTask}，则再遍历所有 Bean 的方法，
     * 为每个带 {@code @RetryTask} 注解的方法克隆一份注册信息并绑定统一处理器；
     * 其余执行器按原生框架逻辑直接注册。
     * </p>
     *
     * @return 重试执行器注册信息列表
     */
    @Override
    public List<RetryerInfo> doScan() {
        List<RetryerInfo> retryerInfoList = new ArrayList<>();
        String[] beanDefinitionNames = applicationContext.getBeanNamesForType(Object.class, false, true);

        for (String beanDefinitionName : beanDefinitionNames) {
            Object bean = applicationContext.getBean(beanDefinitionName);
            ExecutorMethodRegister annotation = bean.getClass().getAnnotation(ExecutorMethodRegister.class);
            if (Objects.nonNull(annotation)) {
                RetryerInfo retryerInfo = resolvingRetryable(annotation, bean);
                Optional.ofNullable(retryerInfo).ifPresent(newRetryerInfo -> {

                    // 扫描到自定义统一重试处理器时，为所有 @RetryTask 方法绑定该处理器
                    if (RetryExecutorTask.class.getName().equals(newRetryerInfo.getExecutorClassName())) {

                        for (String retryBeanDefinitionName : beanDefinitionNames) {
                            Object beanObj = applicationContext.getBean(retryBeanDefinitionName);
                            Class<?> clazz = beanObj.getClass();

                            // 类名含 $$ 说明是 AOP 代理类，取目标原始类以确保能扫描到方法上的注解
                            if (clazz.getName().contains("$$")) {
                                clazz = AopUtils.getTargetClass(beanObj);
                            }

                            for (Method method : clazz.getDeclaredMethods()) {
                                RetryTask retryTask = AnnotationUtils.findAnnotation(method, RetryTask.class);
                                if (Objects.nonNull(retryTask)) {

                                    RetryerInfo copyRetryerInfo = copyRetryerInfo(retryTask, newRetryerInfo);

                                    retryerInfoList.add(copyRetryerInfo);

                                }
                            }
                        }

                    } else {
                        retryerInfoList.add(newRetryerInfo);
                    }

                });
            }
        }

        return retryerInfoList;
    }

    /**
     * 以自定义统一重试处理器注册信息为模板，结合 RetryTask 注解属性构建业务场景注册信息
     * <p>
     * 执行器信息保持不变（重试流量统一回调 {@code RetryExecutorTask.doExecute}），
     * 场景值替换为带版本号前缀的注解业务场景值（{@code RetryExecutorTask.buildScene}，
     * 按应用版本隔离重试流量），上报与重试属性改用 {@code RetryTask} 注解配置，
     * 使业务侧通过 {@code RetryTaskTemplateBuilder} 按业务场景值 + 统一处理器上报时可命中对应注册信息。
     * </p>
     *
     * @param retryTask RetryTask 注解
     * @param retryerInfo 自定义统一重试处理器注册信息
     * @return RetryTask 代理方法对应的重试注册信息
     */
    private RetryerInfo copyRetryerInfo(RetryTask retryTask, RetryerInfo retryerInfo) {
        return new RetryerInfo(RetryExecutorTask.buildScene(retryTask.scene()),
                retryerInfo.getExecutorClassName(),
                retryerInfo.getInclude(),
                retryerInfo.getExclude(),
                retryerInfo.getExecutor(),
                retryerInfo.getMethod(),
                retryTask.retryStrategy(),
                retryTask.localTimes(),
                retryTask.localInterval(),
                retryTask.idempotentId(),
                retryTask.bizNo(),
                retryerInfo.getExecutorMethod(),
                retryerInfo.isThrowException(),
                retryTask.retryCompleteCallback(),
                retryTask.async(),
                retryTask.forceReport(),
                retryTask.timeout(),
                retryTask.unit(),
                retryerInfo.getRetryCondition()
        );
    }

    /**
     * 解析执行器 Bean 上的 {@code @ExecutorMethodRegister} 注解，构建重试注册信息
     * <p>
     * 固定采用仅远程重试策略（{@code RetryType.ONLY_REMOTE}）、本地重试 1 次、
     * 间隔 1（本地不重试，重试交由服务端调度），执行方法固定为
     * {@code doExecute(Object)}，执行异常时抛出，结果判断器为 {@code RetryCondition.NoRetry}。
     * </p>
     *
     * @param retryable 执行器上的 @ExecutorMethodRegister 注解
     * @param executor 执行器 Bean 实例
     * @return 重试注册信息；解析异常（如缺少 doExecute 方法）时记录日志并返回 null
     */
    private RetryerInfo resolvingRetryable(ExecutorMethodRegister retryable, Object executor) {

        try {
            Class executorNotProxy = AopUtils.getTargetClass(executor);
            String executorClassName = executorNotProxy.getName();
            Class<? extends IdempotentIdGenerate> idempotentIdGenerate = retryable.idempotentId();
            Method executorMethodName = executorNotProxy.getMethod("doExecute", Object.class);
            Class<? extends RetryCompleteCallback> retryCompleteCallback = retryable.retryCompleteCallback();
            boolean async = retryable.async();
            long timeout = retryable.timeout();
            TimeUnit unit = retryable.unit();
            boolean forceReport = retryable.forceReport();

            return new RetryerInfo(retryable.scene(),
                    executorClassName,
                    new HashSet<>(Collections.emptyList()),
                    new HashSet<>(Collections.emptyList()),
                    executor,
                    executorMethodName,
                    RetryType.ONLY_REMOTE,
                    1,
                    1,
                    idempotentIdGenerate,
                    retryable.bizNo(),
                    (Class<? extends ExecutorMethod>) executor.getClass(),
                    Boolean.TRUE,
                    retryCompleteCallback,
                    async,
                    forceReport,
                    timeout,
                    unit,
                    RetryCondition.NoRetry.class
            );
        } catch (Exception e) {
            SnailJobLog.LOCAL.error("Error loading retry information for {}: {}", executor.getClass().getName(), e);
        }

        return null;
    }

    /**
     * 注入 Spring 应用上下文（容器回调）
     *
     * @param applicationContext Spring 应用上下文
     * @throws BeansException 接口声明异常，本实现不会抛出
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
