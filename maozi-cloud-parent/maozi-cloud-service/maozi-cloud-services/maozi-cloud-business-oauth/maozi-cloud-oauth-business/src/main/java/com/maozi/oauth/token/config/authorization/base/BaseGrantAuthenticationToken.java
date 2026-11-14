package com.maozi.oauth.token.config.authorization.base;
import lombok.Getter;
import org.springframework.lang.Nullable;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * 自定义授权模式认证令牌基类
 * <p>
 * 作为各种自定义授权模式（如密码模式、短信验证码模式等）的认证令牌基础类。
 * 封装了授权过程中所需的通用信息，包括授权范围（scope）、客户端认证信息、
 * 附加参数和授权类型等。
 * </p>
 *
 * @author vains
 * @date 2026/06/19
 */
@Getter
public class BaseGrantAuthenticationToken extends AbstractAuthenticationToken {

    /**
     * 本次登录申请的scope（授权范围）
     * -- GETTER --
     *  返回请求的scope(s)
     *
     * @return 请求的scope(s)

     */
    private final Set<String> scopes;

    /**
     * 客户端认证信息，包含已认证的客户端详情
     */
    private final Authentication clientPrincipal;

    /**
     * 当前请求的附加参数（如用户名、密码等）
     */
    private final Map<String, Object> additionalParameters;

    /**
     * 认证方式（授权类型），如password、sms等
     */
    private final AuthorizationGrantType authorizationGrantType;

    /**
     * 构造函数，创建自定义授权认证令牌
     *
     * @param authorizationGrantType 授权类型（如密码模式、短信模式等）
     * @param clientPrincipal        客户端认证信息
     * @param scopes                 请求的授权范围，可为空
     * @param additionalParameters   附加参数（如用户名、密码等），可为空
     */
    public BaseGrantAuthenticationToken(AuthorizationGrantType authorizationGrantType,
                                              Authentication clientPrincipal,
                                              @Nullable Set<String> scopes,
                                              @Nullable Map<String, Object> additionalParameters) {
        super(Collections.emptyList());
        this.scopes = scopes;
        this.clientPrincipal = clientPrincipal;
        this.additionalParameters = additionalParameters;
        this.authorizationGrantType = authorizationGrantType;
    }

    /**
     * 获取凭证信息
     * <p>
     * 自定义授权模式中凭证由附加参数承载，此处返回null。
     * </p>
     *
     * @return null
     */
    @Override
    public Object getCredentials() {
        return null;
    }

    /**
     * 获取主体信息
     * <p>
     * 返回客户端认证信息作为主体。
     * </p>
     *
     * @return 客户端认证信息
     */
    @Override
    public Object getPrincipal() {
        return clientPrincipal;
    }

}