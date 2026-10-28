package com.maozi.monitor.config;

import com.maozi.common.spi.ConfigInitializer;

import java.util.Map;
import java.util.Properties;

/**
 * 监控服务运行时配置初始化器
 * <p>
 * 通过 SPI 机制被 {@link com.maozi.BaseApplication} 加载，将监控服务的
 * Nacos 共享配置文件清单追加到系统属性 {@code spring.config.import} 导入列表，
 * 属性已存在时以逗号前插合并。
 * </p>
 *
 * @author maozi
 */
public class MonitorConfigInitializer implements ConfigInitializer {

    /** 监控服务 Nacos 共享配置文件导入清单（optional 前缀保证文件不存在时不阻断启动） */
    private static final String NACOS_IMPORTS = "optional:nacos:cloud-nacos.yml,optional:nacos:boot-monitor.yml,optional:nacos:cloud-default.yml";

    /**
     * 追加监控服务的 Nacos 共享配置文件清单（前插合并，保证应用自身
     * 配置文件始终位于导入列表末尾、优先级最高）
     *
     * @param properties 系统属性
     */
    @Override
    public void initialize(Properties properties) {
        properties.merge(
            "spring.config.import",
            NACOS_IMPORTS,
            (existing, incoming) -> incoming + "," + existing
        );
    }

    /**
     * 本初始化器无需输出额外的启动诊断信息，空实现
     *
     * @param logs 启动日志容器
     */
    @Override
    public void appendLogs(Map<String, String> logs) {}

}
