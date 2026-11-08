package com.maozi.oauth.token.config.authorization.password;

import com.maozi.oauth.token.config.authorization.base.BaseGrantAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

import java.util.Map;
import java.util.Set;

/**
 * 密码模式认证令牌
 * <p>
 * 密码模式（password grant type）的认证令牌实现，
 * 继承自BaseGrantAuthenticationToken，封装密码模式所需的认证信息。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
public class PasswordAuthenticationToken extends BaseGrantAuthenticationToken {

    /**
     * 构造密码模式认证令牌
     *
     * @param authorizationGrantType 授权类型（密码模式）
     * @param clientPrincipal        客户端认证信息
     * @param scopes                 请求的授权范围
     * @param additionalParameters   附加参数（包含用户名和密码）
     */
    public PasswordAuthenticationToken(AuthorizationGrantType authorizationGrantType, Authentication clientPrincipal, Set<String> scopes, Map<String, Object> additionalParameters) {
        super(authorizationGrantType, clientPrincipal, scopes, additionalParameters);
    }
}
