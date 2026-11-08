package com.maozi.common.properties;

import com.maozi.common.CollectionUtil;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * API 白名单配置属性
 * <p>
 * 定义不需要认证即可访问的 API 路径列表。包含系统默认白名单
 * （OAuth2 内省端点、Swagger 文档、Actuator 监控端点等）和
 * 通过配置文件自定义的项目级白名单。
 * </p>
 *
 * @author maozi
 */
@Data
@Component
public class ApiWhitelistProperties {

    /** 系统默认白名单路径列表 */
    public static final List<String> DEFAULT_WHITE_LIST;

    /** 项目自定义白名单路径列表，通过 application-project-whitelist 配置 */
    @Value("${application-project-whitelist:}")
    private List<String> configWhitelist;

    static {

        DEFAULT_WHITE_LIST = CollectionUtil.newArrayList();

        // OAuth2 令牌内省端点，供资源服务器验证令牌有效性
        DEFAULT_WHITE_LIST.add("/oauth2/introspect");
        DEFAULT_WHITE_LIST.add("/oauth/token/**/introspect");
        // Swagger UI 依赖的静态资源路径
        DEFAULT_WHITE_LIST.add("/webjars/**");
        // Spring Boot Actuator 监控端点，用于健康检查和运维监控
        DEFAULT_WHITE_LIST.add("/actuator/**");
        // 应用配置相关端点
        DEFAULT_WHITE_LIST.add("/application/**");
        // OpenAPI 3.0 API 文档端点
        DEFAULT_WHITE_LIST.add("/v3/api-docs/**");

    }

}
