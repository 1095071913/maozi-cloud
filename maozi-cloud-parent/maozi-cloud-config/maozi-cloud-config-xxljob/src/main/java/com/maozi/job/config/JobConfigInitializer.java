package com.maozi.job.config;

import com.maozi.common.spi.ConfigInitializer;

import java.util.Map;
import java.util.Properties;

/**
 * 定时任务运行时配置初始化器
 * <p>
 * 通过 SPI 机制被 {@code BaseApplication} 加载，将 XXL-Job 相关
 * Nacos 共享配置文件追加到系统属性 {@code spring.config.import} 导入列表。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
public class JobConfigInitializer implements ConfigInitializer {

    /** 定时任务相关的 Nacos 共享配置文件导入（optional 前缀保证文件不存在时不阻断启动） */
    private static final String NACOS_IMPORTS = "optional:nacos:boot-xxljob.yml";

    /**
     * 追加 XXL-Job 共享配置文件到 Nacos 配置导入列表。
     * <p>
     * 若 {@code spring.config.import} 已有值，则以逗号分隔前插合并，
     * 保留其他模块已注册的共享配置文件，并保证应用自身配置文件优先级最高。
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

    /** 空实现，XXL-Job 配置初始化无需向启动日志追加诊断信息。 */
    @Override
    public void appendLogs(Map<String, String> logs) {}

}
