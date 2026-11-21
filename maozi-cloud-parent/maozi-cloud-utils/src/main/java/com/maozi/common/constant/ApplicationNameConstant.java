package com.maozi.common.constant;

/**
 * 应用服务名常量类
 * <p>
 * 集中定义项目内各微服务的服务名称，供服务发现、Feign 客户端声明
 * 以及运行环境相关判断使用。
 * </p>
 *
 * @author pengjinlong
 * @date 2026/7/8
 */
public final class ApplicationNameConstant {

    /** 私有构造器，防止常量类被实例化 */
    private ApplicationNameConstant() {}

    /** 单体全量服务名（所有服务合并为单个应用部署） */
    public static final String MAOZI_CLOUD_ALL_SERVICE = "maozi-cloud-all-service";

    /** 系统管理服务名 */
    public static final String MAOZI_CLOUD_SYSTEM_SERVICE = "maozi-cloud-system-service";

    /** OAuth 认证服务名 */
    public static final String MAOZI_CLOUD_OAUTH_SERVICE = "maozi-cloud-oauth-service";

}
