package com.maozi.snailjob.retry.annotation;

import com.aizuda.snailjob.client.core.IdempotentIdGenerate;
import com.aizuda.snailjob.client.core.RetryCondition;
import com.aizuda.snailjob.client.core.callback.complete.RetryCompleteCallback;
import com.aizuda.snailjob.client.core.callback.complete.SimpleRetryCompleteCallback;
import com.aizuda.snailjob.client.core.generator.SimpleIdempotentIdGenerate;
import com.aizuda.snailjob.client.core.retryer.RetryType;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 自定义重试注解
 * <p>
 * 标注在需要自定义重试能力的方法上。方法首次执行失败（抛出异常或结果集判定需要重试）时，
 * 按上报策略先在本地按次数与间隔同步重试，仍失败则将方法信息封装上报 SnailJob 服务端，
 * 由统一重试处理器 {@code RetryExecutorTask} 在服务端调度时远程回调执行。
 * </p>
 * <p>
 * 服务端注册相关属性（scene、idempotentId、retryCompleteCallback、bizNo、timeout、unit、
 * forceReport、async）与 {@code @ExecutorMethodRegister} 注解属性语义一致，
 * 在 {@code ExecutorMethodScanner} 扫描统一重试处理器时按本注解配置复制注册。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RetryTask {

    /**
     * 场景（服务端重试任务分组标识，同组内唯一）
     * <p>
     * 重试注册与上报时自动拼接版本号前缀（{@code ApplicationEnvironmentContext.VERSION + "_" + 场景值}），
     * 按应用版本隔离不同版本服务间的重试流量。
     * </p>
     *
     * @return 场景值
     */
    String scene();

    /**
     * 幂等 id 生成器
     *
     * @return 幂等 id 生成器类型
     */
    Class<? extends IdempotentIdGenerate> idempotentId() default SimpleIdempotentIdGenerate.class;

    /**
     * 服务端重试完成（重试成功、重试到达最大次数）回调客户端处理器
     *
     * @return 重试完成回调处理器类型
     */
    Class<? extends RetryCompleteCallback> retryCompleteCallback() default SimpleRetryCompleteCallback.class;

    /**
     * 标识具有业务特点的值，比如订单号、物流编号等，可以根据具体的业务场景生成
     *
     * @return 业务标识值
     */
    String bizNo() default "";

    /**
     * 同步（async 为 false）上报数据需要配置超时时间
     *
     * @return 超时时间
     */
    long timeout() default 60 * 1000;

    /**
     * 超时时间单位
     *
     * @return 超时时间单位
     */
    TimeUnit unit() default TimeUnit.MILLISECONDS;

    /**
     * 是否强制上报数据到服务端
     *
     * @return 是否强制上报
     */
    boolean forceReport() default false;

    /**
     * 是否异步上报数据到服务端
     *
     * @return 是否异步上报
     */
    boolean async() default true;

    /**
     * 本地重试次数（首次执行失败后本地额外重试的次数）
     *
     * @return 本地重试次数
     */
    int localTimes() default 2;

    /**
     * 本地重试间隔时间（秒），防止重试过快导致结果与上一次一致
     *
     * @return 本地重试间隔时间
     */
    int localInterval() default 2;

    /**
     * 上报策略：仅本地重试、仅远程重试、本地与远程重试结合
     *
     * @return 上报策略
     */
    RetryType retryStrategy() default RetryType.LOCAL_REMOTE;

    /**
     * 重试结果判断器（需注册为 Spring Bean），返回 true 表示结果集需要重试
     *
     * @return 重试结果判断器类型
     */
    Class<? extends RetryCondition> retryIfResult() default RetryCondition.NoRetry.class;

}
