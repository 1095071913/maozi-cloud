/*
 * Copyright (C) 2018 Zhejiang xiaominfo Technology CO.,LTD.
 * All rights reserved.
 * Official Web Site: http://www.xiaominfo.com.
 * Developer Web Site: http://open.xiaominfo.com.
 */

package com.maozi.swagger.config;

import com.maozi.common.CollectionUtil;
import com.maozi.common.constant.AuthorizationConstant;
import com.maozi.common.context.ApplicationEnvironmentContext;
import com.maozi.common.properties.ApiWhitelistProperties;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.parameters.Parameter;
import jakarta.annotation.Resource;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.HandlerMethod;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Swagger/OpenAPI 文档配置
 * <p>
 * 配置 OpenAPI 文档的基本信息、全局 Authorization 请求头和
 * Long 类型字段转 String 的 Schema 自定义。
 * 白名单接口自动跳过 Authorization 头注入。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@SuppressWarnings({"rawtypes", "unchecked"})
@Configuration
@ConditionalOnProperty(
        name = "springdoc.api-docs.enabled",                 // 配置项名
        havingValue = "true",
        matchIfMissing = true                 // 没配置时默认生效
)
public class SwaggerConfig {

    /** 许可证名称 */
    private static final String LICENSE_NAME = "Apache 2.0";

    /** 授权令牌示例值 */
    private static final String AUTHORIZATION_VALUE = "{{oauth_access_token}}";

    /** 项目主页 URL */
    private static final String OFFICIAL_URL = "https://github.com/1095071913";

    /** OpenAPI Schema int64 格式标识 */
    private static final String FORMAT_INT64 = "int64";

    /** OpenAPI Schema string 类型标识 */
    private static final String TYPE_STRING = "string";

    /** API 白名单配置 */
    @Resource
    private ApiWhitelistProperties apiWhitelistProperties;

    /**
     * 创建 OpenAPI 文档配置
     *
     * @return OpenAPI 文档实例
     */
    @Bean
    public OpenAPI openApi() {
        return new OpenAPI().info(
            new Info()
                .title(ApplicationEnvironmentContext.title)
                .version(ApplicationEnvironmentContext.version)
                .description(ApplicationEnvironmentContext.details)
                .termsOfService(OFFICIAL_URL)
                .license(new License().name(LICENSE_NAME).url(OFFICIAL_URL))
        );
    }

    /**
     * 创建全局 Authorization 请求头操作定制器
     * <p>
     * 为所有非白名单接口自动添加 Authorization 请求头参数，
     * 方便在 Swagger UI 中进行需要认证的接口测试。
     * </p>
     *
     * @return 操作定制器
     */
    @Bean
    public OperationCustomizer globalHeaderOperation() {

        // 白名单 = 系统默认白名单 + 配置文件自定义白名单
        List<String> whitelist = CollectionUtil.newArrayList();
        whitelist.addAll(ApiWhitelistProperties.DEFAULT_WHITE_LIST);

        // 合并项目自定义白名单（可能未配置）
        List<String> apiWhitelist = apiWhitelistProperties.getConfigWhitelist();
        if(CollectionUtil.isNotEmpty(apiWhitelist)){
            whitelist.addAll(apiWhitelist);
        }

        return (Operation operation, HandlerMethod handlerMethod) -> {

            // 取方法 @RequestMapping 声明的第一个路径（优先 path 属性，未配置时回退 value），
            // 未声明路径或白名单内的接口不注入认证头
            RequestMapping requestMapping = handlerMethod.getMethodAnnotation(RequestMapping.class);
            String[] paths = Objects.isNull(requestMapping) ? new String[0]
                    : (requestMapping.path().length > 0 ? requestMapping.path() : requestMapping.value());
            if (paths.length == 0 || whitelist.contains(paths[0])) {
                return operation;
            }

            // 构建全局 Authorization 请求头参数，示例值便于在 Swagger UI 中直接调试
            Parameter tokenHeader = new HeaderParameter()
                    .name(AuthorizationConstant.AUTHORIZATION_HEADER)
                    .example(AUTHORIZATION_VALUE)
                    .description("用户授权令牌")
                    .required(true)
                    .schema(new io.swagger.v3.oas.models.media.StringSchema());
            operation.addParametersItem(tokenHeader);

            return operation;

        };

    }

    /**
     * 创建 Long 转 String 的 Schema 定制器
     * <p>
     * 遍历 OpenAPI Schema 中的所有字段，将 int64 格式的字段类型转为 string，
     * 避免前端 JavaScript 中 Long 类型精度丢失问题。
     * </p>
     *
     * @return OpenAPI 定制器
     */
    @Bean
    public OpenApiCustomizer longToStringResult() {

        return openApi -> {

            Components components = openApi.getComponents();
            if (components == null || components.getSchemas() == null) {
                return;
            }

            Map<String, Schema> schemas = components.getSchemas();
            if (CollectionUtil.isEmpty(schemas)) {
                return;
            }

            for (Schema<?> schema : schemas.values()) {

                Map<String, Schema> properties = schema.getProperties();
                if (properties == null) {
                    continue;
                }
                properties.forEach((fieldName, fieldSchema) -> {
                    // Long 字段在文档中呈现为 int64，改为 string 避免 JS 精度丢失
                    if (FORMAT_INT64.equals(fieldSchema.getFormat())) {
                        Set<String> types = CollectionUtil.newHashSet();
                        types.add(TYPE_STRING);
                        fieldSchema.setTypes(types);
                        fieldSchema.setFormat(null);
                    }
                });
            }

        };
    }

}
