package com.maozi.common.constant;

/**
 * 权限认证相关常量
 * <p>
 * 集中定义 OAuth2 / JWT 等认证流程中使用的请求头、约定 key 等常量。
 * </p>
 *
 * @author pengjinlong
 * @since 2026/6/15 11:38
 */
public final class AuthorizationConstant {

    /** 私有构造器，防止常量类被实例化 */
    private AuthorizationConstant() {}

    /** 标准认证请求头名称，用于携带 Access Token / Bearer 凭证 */
    public static final String AUTHORIZATION_HEADER = "Authorization";

}
