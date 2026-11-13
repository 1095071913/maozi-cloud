package com.maozi.snailjob.retry.param;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 自定义重试任务参数
 * <p>
 * {@code @RetryTask} 方法本地重试未启用或失败后上报 SnailJob 服务端时携带的方法信息，
 * 由统一重试处理器 {@code RetryExecutorTask} 在服务端调度时依据本参数动态回调目标方法。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RetryTaskParam implements Serializable {
    /** 序列化标识 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 重试方法所在 Bean 的名称 */
    @Schema(description = "重试方法所在 Bean 名称")
    private String beanName;

    /** 重试方法名 */
    @Schema(description = "重试方法名")
    private String methodName;

    /** 重试方法参数类型列表 */
    @Schema(description = "重试方法参数类型")
    private Class<?>[] paramTypes;

    /** 重试方法入参数据 */
    @Schema(description = "重试方法入参数据")
    private Object[] data;

    /** 上报时保存的链路追踪 Id */
    @Schema(description = "链路追踪Id")
    private String traceId;

    /** 上报时保存的应用版本号 */
    @Schema(description = "版本号")
    private String version;

    /** 上报时保存的测试流量标记 */
    @Schema(description = "是否测试流量")
    private Boolean testFlow;

    /** 重试结果判断器类名，回调后据此判断结果集是否仍需重试 */
    @Schema(description = "重试结果判断器类名")
    private String retryIfResultClassName;

}
