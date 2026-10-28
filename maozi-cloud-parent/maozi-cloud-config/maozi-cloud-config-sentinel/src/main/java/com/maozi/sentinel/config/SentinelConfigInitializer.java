package com.maozi.sentinel.config;

import com.maozi.BaseApplication;
import com.maozi.common.spi.ConfigInitializer;

import java.util.Map;
import java.util.Properties;

/**
 * Sentinel 运行时配置初始化器
 * <p>
 * 通过 SPI 机制被 {@code BaseApplication} 加载，将 Sentinel 共享配置文件
 * {@code cloud-sentinel.yml} 前插到 {@code spring.config.import} 导入列表，
 * 并屏蔽 Sentinel 初始化日志输出。
 * Sentinel 初始化时通过标准输出直写形如 {@code INFO: Sentinel log ...} 的提示信息，
 * 无官方关闭开关，本初始化器将该特征注册到 {@code BaseApplication} 的统一
 * 过滤列表，由其启动时安装的标准输出包装器丢弃对应输出。
 * </p>
 *
 * @author maozi
 */
public class SentinelConfigInitializer implements ConfigInitializer {

    /** Sentinel 相关的 Nacos 共享配置文件导入（optional 前缀保证文件不存在时不阻断启动） */
    private static final String IMPORTS = "optional:nacos:cloud-sentinel.yml";

    /** Sentinel 初始化日志特征片段，用于识别并丢弃对应的标准输出 */
    private static final String BANNER_FLAG = "INFO: Sentinel log";

    /**
     * 导入 Sentinel 共享配置文件，并屏蔽 Sentinel 初始化日志输出。
     * <p>
     * 先将 {@code cloud-sentinel.yml} 以 optional 前缀合并到
     * {@code spring.config.import} 导入列表；再将 Sentinel 初始化日志特征
     * 注册到 {@code BaseApplication} 的统一过滤列表，由其启动时安装的
     * 标准输出包装器丢弃对应输出。
     * </p>
     *
     * @param properties 系统属性容器
     */
    @Override
    public void initialize(Properties properties) {
        properties.merge(
                "spring.config.import",
                IMPORTS,
                (existing, incoming) -> incoming + "," + existing
        );
        BaseApplication.addBannerFilter(BANNER_FLAG);
    }

    /** 空实现，Sentinel 配置初始化无需向启动日志追加诊断信息。 */
    @Override
    public void appendLogs(Map<String, String> logs) {}

}
