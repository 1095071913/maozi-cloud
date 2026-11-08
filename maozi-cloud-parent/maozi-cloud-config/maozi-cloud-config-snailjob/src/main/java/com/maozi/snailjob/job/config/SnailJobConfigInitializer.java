package com.maozi.snailjob.job.config;

import com.maozi.BaseApplication;
import com.maozi.common.spi.ConfigInitializer;

import java.util.Map;
import java.util.Properties;

/**
 * 任务调度与重试运行时配置初始化器
 * <p>
 * 通过 SPI 机制被 {@code BaseApplication} 加载，将 SnailJob 相关
 * Nacos 共享配置文件追加到系统属性 {@code spring.config.import} 导入列表，
 * 并注册 SnailJob 启动 banner 过滤特征。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
public class SnailJobConfigInitializer implements ConfigInitializer {

    /** 任务调度与重试相关的 Nacos 共享配置文件导入（optional 前缀保证文件不存在时不阻断启动） */
    private static final String NACOS_IMPORTS = "optional:nacos:boot-snailjob.yml";

    /** SnailJob 启动 banner 特征片段，用于识别并丢弃 banner 输出 */
    private static final String BANNER_FLAG = ":: Snail Job ::";

    /**
     * 追加 SnailJob 共享配置文件到 Nacos 配置导入列表，并注册 banner 过滤特征。
     * <p>
     * 若 {@code spring.config.import} 已有值，则以逗号分隔前插合并，
     * 保留其他模块已注册的共享配置文件，并保证应用自身配置文件优先级最高。
     * </p>
     * <p>
     * SnailJob 的 {@code SnailJobStartListener} 在启动时无条件通过
     * {@code System.out.println} 输出 banner，官方未提供关闭开关且不走日志框架，
     * 故将 banner 特征注册到 {@code BaseApplication} 的统一过滤列表，
     * 由其启动时安装的标准输出包装器丢弃对应输出。
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

        BaseApplication.addBannerFilter(BANNER_FLAG);
    }

    /** 空实现，SnailJob 配置初始化无需向启动日志追加诊断信息。 */
    @Override
    public void appendLogs(Map<String, String> logs) {}

}
