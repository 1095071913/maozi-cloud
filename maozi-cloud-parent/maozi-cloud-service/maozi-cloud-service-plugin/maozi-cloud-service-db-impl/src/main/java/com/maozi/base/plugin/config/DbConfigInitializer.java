package com.maozi.base.plugin.config;

import com.maozi.common.spi.ConfigInitializer;

import java.util.Map;
import java.util.Properties;

/**
 * 数据库运行时配置初始化器
 * <p>
 * 通过 SPI 机制被 {@link com.maozi.BaseApplication} 加载，将数据库相关
 * Nacos 共享配置文件追加到系统属性 {@code spring.config.import} 导入列表。
 * </p>
 *
 * @author maozi
 */
public class DbConfigInitializer implements ConfigInitializer {

    /** 数据库相关 Nacos 共享配置文件导入清单（optional 前缀保证文件不存在时不阻断启动） */
    private static final String NACOS_IMPORTS = "optional:nacos:boot-db.yml";

    /**
     * 初始化数据库运行时配置
     * <p>
     * 将数据库相关 Nacos 共享配置文件追加到系统属性
     * {@code spring.config.import}；采用前插方式合并，保证应用自身配置文件
     * （由 maozi-cloud-config-nacos 后插）始终位于列表末尾、优先级最高。
     * </p>
     *
     * @param properties 系统属性容器
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
     * 本初始化器无需输出额外的启动诊断日志
     *
     * @param logs 启动日志容器（未使用）
     */
    @Override
    public void appendLogs(Map<String, String> logs) {}

}
