package com.maozi.dubbo.config;

import com.maozi.BaseApplication;
import com.maozi.common.spi.ConfigInitializer;

import java.util.Map;
import java.util.Properties;

/**
 * Dubbo 运行时配置初始化器
 * <p>
 * 通过 SPI 机制被 {@code BaseApplication} 加载，屏蔽 Dubbo 启动 banner 输出。
 * Dubbo 的 {@code WelcomeLogoApplicationListener} 无官方关闭开关，输出逻辑为：
 * INFO 级别可用时走日志框架输出，否则回退为 {@code System.out.print} 直写标准输出。
 * 本初始化器将 banner 日志记录器级别设为 OFF，强制统一走标准输出回退路径
 * （避免 banner 以 INFO 日志形式进入控制台与日志文件），并将 banner 特征
 * 注册到 {@code BaseApplication} 的统一过滤列表，由其启动时安装的标准输出
 * 包装器丢弃对应输出。
 * </p>
 *
 * @author maozi
 */
public class DubboConfigInitializer implements ConfigInitializer {

    /** Dubbo banner 输出类的全路径，该类仅有 banner 一处日志输出，整类关闭无副作用 */
    private static final String WELCOME_LOGO_LOGGER = "org.apache.dubbo.spring.boot.context.event.WelcomeLogoApplicationListener";

    /** Dubbo 启动 banner 特征片段，用于识别并丢弃 banner 输出 */
    private static final String BANNER_FLAG = ":: Dubbo (v";

    /**
     * 屏蔽 Dubbo 启动 banner 输出。
     * <p>
     * 写入日志记录器关闭属性：Spring Boot 在环境准备阶段
     * （早于 banner 输出监听器执行）应用 {@code logging.level.*} 属性，
     * 使 banner 记录器 INFO 级别不可用，强制走标准输出回退路径。
     * 同时将 banner 特征注册到 {@code BaseApplication} 的统一过滤列表。
     * </p>
     *
     * @param properties 系统属性容器
     */
    @Override
    public void initialize(Properties properties) {

        properties.put("logging.level." + WELCOME_LOGO_LOGGER, "OFF");

        properties.put("dubbo.module.keepRunningOnSpringClosed",true);

        BaseApplication.addBannerFilter(BANNER_FLAG);

    }

    /** 空实现，Dubbo 配置初始化无需向启动日志追加诊断信息。 */
    @Override
    public void appendLogs(Map<String, String> logs) {}

}
