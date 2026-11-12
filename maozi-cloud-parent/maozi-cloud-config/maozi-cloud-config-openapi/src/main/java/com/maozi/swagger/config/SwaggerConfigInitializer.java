package com.maozi.swagger.config;

import com.maozi.common.constant.RemoteConstant;
import com.maozi.common.enums.EnvironmentType;
import com.maozi.common.spi.ConfigInitializer;

import java.util.Map;
import java.util.Properties;

/**
 * Swagger/OpenAPI 配置初始化器
 * <p>
 * 根据当前运行环境动态设置 {@code springdoc.api-docs.enabled} 属性：
 * 生产环境（prod）关闭 API 文档暴露，非生产环境开启，避免生产环境泄露接口元数据。
 * </p>
 * <p>
 * 同时通过 {@code springdoc.paths-to-exclude} 过滤掉以 {@code /remote} 开头的接口路径，
 * 这些通常是服务间内部调用接口（如 Feign 适配的 REST 接口），无需对外暴露文档。
 * 此处通过 SPI 初始化器设置，确保在任何环境下都能稳定生效。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
public class SwaggerConfigInitializer implements ConfigInitializer {

    /** 排除的接口路径前缀（Ant 风格），用于屏蔽内部 /remote 接口文档 */
    private static final String EXCLUDE_REMOTE_PATHS = RemoteConstant.REMOTE_PREFIX_PATH + "/**";

    /**
     * 根据运行环境设置 API 文档开关，并排除内部接口路径。
     * <p>
     * 当前环境为生产环境时关闭 {@code springdoc.api-docs.enabled}，
     * 其余环境开启；同时将 {@code /remote} 前缀路径加入
     * {@code springdoc.paths-to-exclude}，屏蔽服务间内部调用接口的文档。
     * </p>
     *
     * @param properties 系统属性容器
     */
    @Override
    public void initialize(Properties properties) {
        String environment = (String) properties.get("environment");
        properties.put("springdoc.api-docs.enabled", !EnvironmentType.PROD.getDesc().equals(environment));
        properties.put("springdoc.paths-to-exclude", EXCLUDE_REMOTE_PATHS);
    }

    /** 空实现，Swagger 配置初始化无需向启动日志追加诊断信息。 */
    @Override
    public void appendLogs(Map<String, String> logs) {}

}
