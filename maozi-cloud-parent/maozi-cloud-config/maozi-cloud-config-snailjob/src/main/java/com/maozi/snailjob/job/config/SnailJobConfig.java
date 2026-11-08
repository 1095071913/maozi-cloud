package com.maozi.snailjob.job.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.aizuda.snailjob.client.common.appender.SnailLogbackAppender;
import com.aizuda.snailjob.client.starter.EnableSnailJob;
import jakarta.annotation.PostConstruct;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

/**
 * SnailJob 分布式任务调度与重试配置
 * <p>
 * 通过 {@code @EnableSnailJob} 开启 SnailJob 客户端支持，
 * 由 snail-job-client-starter 依据 {@code snail-job} 前缀的配置
 * （服务端地址、组名、端口等，来源于 Nacos 共享配置 boot-snailjob.yml）
 * 自动装配任务调度与重试相关组件。
 * </p>
 * <p>
 * 同时在类加载时屏蔽 Fory 序列化框架（SnailJob 底层依赖）的冗余日志
 * （类注册告警、实例创建、序列化代码生成、白名单提示），
 * 在容器初始化时将 SnailJob 日志上报 appender 挂载到 Logback 根日志记录器，
 * 将任务与重试执行过程中的日志上报至 SnailJob 服务端。
 * 本配置类随 maozi-cloud-config-snailjob 依赖引入即生效，服务无需任何额外配置。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
@Configuration
@EnableSnailJob
public class SnailJobConfig {

    /** SnailJob 日志上报 appender 名称，用于挂载前判重，避免重复挂载 */
    private static final String SNAIL_LOG_APPENDER_NAME = "snailLogbackAppender";

    /** Fory 序列化框架（SnailJob 底层依赖）的日志命名空间 */
    private static final String FORY_LOGGER_NAME = "org.apache.fory";

    /** SnailJob 客户端远程重试执行器（RemoteRetryExecutor）的日志命名空间 */
    private static final String REMOTE_RETRY_EXECUTOR_LOGGER_NAME = "com.aizuda.snailjob.client.core.executor.RemoteRetryExecutor";

    static {
        // Fory 默认使用自带的 ForyLogger 直接 System.out 打印日志（自带格式，绕过 SLF4J/Logback，
        // 应用侧无论怎么调 Logback 级别都无法管控），必须在其首次创建内部 Logger 前切换为
        // SLF4J 实现，才能纳入应用统一日志体系；本静态块在配置类加载时执行，
        // 早于 SnailJob 客户端 Bean 初始化与 Fory 类加载，时序可靠。
        org.apache.fory.logging.LoggerFactory.useSlf4jLogging(true);
        // Fory 的类注册告警、实例创建、序列化代码生成与白名单提示均为框架内部行为，与业务无关，
        // 全局级别压至 ERROR 屏蔽；该级别在 Fory 每次打日志时动态校验，对已创建的 ForyLogger 同样生效
        org.apache.fory.logging.LoggerFactory.setLogLevel(org.apache.fory.logging.LogLevel.ERROR_LEVEL);
        // 双保险：SLF4J 通道下再由 Logback 级别兜底（当前日志实现非 Logback 时不做处理）
        if (LoggerFactory.getILoggerFactory() instanceof LoggerContext loggerContext) {
            loggerContext.getLogger(FORY_LOGGER_NAME).setLevel(Level.ERROR);
        }
    }

    /**
     * 挂载 SnailJob 日志上报 appender 到 Logback 根日志记录器，并屏蔽远程重试结果日志。
     * <p>
     * 此时 Spring Boot 已完成 logback-spring.xml 解析（发生在环境准备阶段，早于容器刷新），
     * 此后挂载的 appender 与调整的日志级别不会被日志系统重置清除。
     * 该 appender 仅上报任务与重试执行上下文内的日志，容器初始化前的启动日志不受影响。
     * 当前日志实现非 Logback 时不做任何处理。
     * </p>
     * <p>
     * 屏蔽依赖包 {@code RemoteRetryExecutor} 输出的远程重试结果日志
     * （remote retry 【SUCCESS】/【STOP】/【FAILURE】/【UNKNOWN】，含失败时的异常堆栈），
     * 测试重试场景（如 test_sj_retry 的 1/0）会持续产生 ERROR 刷屏，故整体关闭该 logger。
     * 重试失败信息仍可经 SnailJob 服务端控制台与自研重试执行器日志观测。
     * </p>
     */
    @PostConstruct
    public void attachSnailLogAppender() {

        ILoggerFactory loggerFactory = LoggerFactory.getILoggerFactory();
        if (!(loggerFactory instanceof LoggerContext loggerContext)) {
            return;
        }

        // 屏蔽 SnailJob 客户端远程重试结果日志，须在 appender 判重返回前执行，保证始终生效
        loggerContext.getLogger(REMOTE_RETRY_EXECUTOR_LOGGER_NAME).setLevel(Level.OFF);

        Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
        if (rootLogger.getAppender(SNAIL_LOG_APPENDER_NAME) != null) {
            return;
        }

        SnailLogbackAppender<ILoggingEvent> appender = new SnailLogbackAppender<>();
        appender.setContext(loggerContext);
        appender.setName(SNAIL_LOG_APPENDER_NAME);
        appender.start();
        rootLogger.addAppender(appender);
    }

}
