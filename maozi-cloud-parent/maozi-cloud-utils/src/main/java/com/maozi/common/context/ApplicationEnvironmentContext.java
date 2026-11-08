package com.maozi.common.context;

import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * 应用环境上下文
 * <p>
 * 存储应用运行时的全局环境信息，包括项目缩写、服务名称、Nacos 配置地址、
 * 环境类型、项目标题、项目版本、项目描述，以及 Spring {@link Environment} 实例。
 * 使用 {@link RefreshScope} 支持配置动态刷新。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
@Data
@Component(ApplicationEnvironmentContext.CLASS_NAME)
@RefreshScope(proxyMode = ScopedProxyMode.NO)
public class ApplicationEnvironmentContext {

    /** Spring Bean 名称常量，用于 {@code @Component(CLASS_NAME)} 显式命名 */
    public static final String CLASS_NAME = "applicationEnvironmentContext";

    /** 基础包路径前缀 */
    public static final String PACKAGE_PREFIX = "com.maozi";

    /** 项目缩写 */
    public static String APPLICATION_PROJECT_ABBREVIATION;

    @Value("${application-project-abbreviation}")
    public void setApplicationProjectAbbreviation(String applicationProjectAbbreviation) {
        ApplicationEnvironmentContext.APPLICATION_PROJECT_ABBREVIATION = applicationProjectAbbreviation;
    }

    /** 服务名称 */
    public static String SERVICE_NAME;

    @Value("${spring.application.name}")
    public void setServiceName(String serviceName) {
        ApplicationEnvironmentContext.SERVICE_NAME = serviceName;
    }

    /** Nacos 配置中心地址 */
    public static String CONFIG_ADDR;

    @Value("${spring.cloud.nacos.config.server-addr:127.0.0.1:8848}")
    public void setConfigAddr(String configAddr) {
        ApplicationEnvironmentContext.CONFIG_ADDR = configAddr;
    }

    /** 当前运行环境 */
    public static String ENVIRONMENT;

    @Value("${application.environment:local}")
    public void setEnvironment(String environment) {
        ApplicationEnvironmentContext.ENVIRONMENT = environment;
    }

    /** 项目标题 */
    public static String TITLE;

    @Value("${application.title:}")
    public void setTitle(String title) {
        ApplicationEnvironmentContext.TITLE = title;
    }

    /** 项目版本 */
    public static String VERSION;

    @Value("${application.version:main}")
    public void setVersion(String version) {
        ApplicationEnvironmentContext.VERSION = version;
    }

    /** 项目描述 */
    public static String DETAILS;

    @Value("${application.details:}")
    public void setDetails(String details) {
        ApplicationEnvironmentContext.DETAILS = details;
    }

    /** Spring Environment 实例 */
    public static Environment CONFIG;

    @Resource
    public void setEnvironmentConfig(Environment environmentConfig) {
        ApplicationEnvironmentContext.CONFIG = environmentConfig;
    }

}
