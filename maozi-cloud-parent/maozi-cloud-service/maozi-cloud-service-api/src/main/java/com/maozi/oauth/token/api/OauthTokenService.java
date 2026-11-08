package com.maozi.oauth.token.api;

import java.util.Map;

/**
 * OAuth 令牌服务接口
 * <p>
 * 定义访问令牌（Access Token）的内省能力，供 {@code OpaqueTokenIntrospector} 校验令牌有效性。
 * 授权服务器进程使用本地实现 {@code OauthTokenServiceImpl}；资源服务器进程使用
 * {@code RemoteOauthTokenServiceImpl}，按配置经 Dubbo RPC（默认）或 REST（Feign）远程调用授权服务器。
 * </p>
 *
 * @author pengjinlong
 * @since 2026/6/24 23:55
 */
public interface OauthTokenService {

    /**
     * 内省访问令牌
     * <p>
     * 校验令牌有效性并返回令牌关联的声明信息，供资源服务器完成身份认证。
     * </p>
     *
     * @param token 待内省的令牌字符串
     * @return 令牌内省结果，包含 active、sub、authorities 等字段，
     *         当令牌无效时 active 为 false
     */
    Map<String, Object> introspect(String token);

}
