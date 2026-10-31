package com.maozi.gateway.sentinel;

import com.alibaba.cloud.sentinel.gateway.ConfigConstants;
import com.maozi.BaseApplication;
import com.maozi.common.spi.ConfigInitializer;

import java.util.Map;
import java.util.Properties;

/**
 * Sentinel 网关配置初始化器
 * <p>
 * 通过 SPI 机制被 {@code BaseApplication} 加载，完成两项配置：
 * 一是将系统属性 {@code csp.sentinel.app.type} 设置为 Spring Cloud Gateway
 * 应用类型，使 Sentinel 将本服务识别为网关应用；二是将 Sentinel 启动日志
 * 特征注册到 {@code BaseApplication} 的统一过滤列表，由其启动时安装的标准
 * 输出包装器丢弃对应输出，屏蔽 Sentinel 启动日志对控制台的干扰。
 * </p>
 *
 * @author maozi
 */
public class SentinelConfigInitializer implements ConfigInitializer {

    /** Sentinel 启动日志特征片段，用于识别并丢弃对应输出 */
    private static final String BANNER_FLAG = "INFO: Sentinel log";

    /**
     * 设置 Sentinel 网关应用类型并过滤其启动日志输出。
     * <p>
     * 写入系统属性 {@code csp.sentinel.app.type} 为 Spring Cloud Gateway
     * 应用类型，使 Sentinel 以网关应用初始化；同时将 Sentinel 启动日志
     * 特征注册到 {@code BaseApplication} 的统一过滤列表，由其标准输出
     * 包装器丢弃对应输出。
     * </p>
     *
     * @param properties 系统属性容器
     */
    @Override
    public void initialize(Properties properties) {
        properties.setProperty("csp.sentinel.app.type", ConfigConstants.APP_TYPE_SCG_GATEWAY);
        properties.put("csp.sentinel.heartbeat.api.path","/sentinel/registry/machine");
        BaseApplication.addBannerFilter(BANNER_FLAG);
    }

    /** 空实现，Sentinel 配置初始化无需向启动日志追加诊断信息。 */
    @Override
    public void appendLogs(Map<String, String> logs) {}

}
