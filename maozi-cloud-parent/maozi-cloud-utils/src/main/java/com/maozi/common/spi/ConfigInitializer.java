package com.maozi.common.spi;

import java.util.Map;
import java.util.Properties;

/**
 * 运行时配置初始化器
 * <p>
 * 基于 SPI 机制，由各业务模块实现此接口，并通过
 * {@code META-INF/run/config/} 目录下的 .properties 文件注册实现类全路径。
 * 启动时统一扫描、实例化并依次调用所有实现，
 * 将配置写入系统属性，并可向启动日志贡献诊断信息。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
public interface ConfigInitializer {

    /**
     * 初始化运行时配置
     *
     * @param properties 系统属性容器，实现类将需注入的配置项写入此 Properties
     */
    void initialize(Properties properties);

    /**
     * 追加启动日志
     * <p>
     * 在 {@link #initialize(Properties)} 执行之后调用，无论成功或失败均会触发，
     * 实现类可将本次初始化的诊断信息写入此 Map 以便统一输出。
     * </p>
     *
     * @param logs 启动日志容器
     */
    void appendLogs(Map<String, String> logs);

}
