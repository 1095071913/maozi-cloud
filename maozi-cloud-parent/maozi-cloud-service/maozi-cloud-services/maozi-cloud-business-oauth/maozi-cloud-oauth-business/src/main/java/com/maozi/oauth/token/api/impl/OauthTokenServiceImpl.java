package com.maozi.oauth.token.api.impl;

import com.maozi.common.CollectionUtil;
import com.maozi.common.ObjectUtil;
import com.maozi.oauth.token.api.OauthTokenService;
import com.maozi.oauth.token.config.service.OAuth2AuthorizationService;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Resource;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2TokenIntrospectionClaimNames;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * OAuth 令牌服务本地实现
 * <p>
 * 基于授权记录实现访问令牌内省逻辑，校验令牌有效性并返回声明信息；
 * 由 RPC/REST 实现层对外暴露，供资源服务器完成令牌校验。
 *
 * @author maozi
 * @date 2026/6/24 23:58
 */
@Service("oauthTokenService")
public class OauthTokenServiceImpl implements OauthTokenService {

    /** OAuth2授权信息服务，用于管理授权记录的存储、查找和删除 */
    @Resource
    protected OAuth2AuthorizationService authorizationService;

    /** OAuth2 客户端注册信息仓库，用于按 clientId 查询已注册客户端 */
    @Resource
    protected RegisteredClientRepository registeredClientRepository;

    /**
     * 内省访问令牌，令牌无效（授权记录不存在、已失效或客户端未注册）时返回 active=false
     */
    @Override
    public Map<String, Object> introspect(String token) {

        // 通过access_token查找对应的授权记录
        OAuth2Authorization authorization = authorizationService.findByToken(token, OAuth2TokenType.ACCESS_TOKEN);
        if (authorization == null) {
            return CollectionUtil.newHashMap(OAuth2TokenIntrospectionClaimNames.ACTIVE, false);
        }

        // 校验授权记录中的AccessToken是否有效
        OAuth2Authorization.Token<OAuth2AccessToken> accessToken = authorization.getAccessToken();
        if (accessToken == null || !accessToken.isActive()) {
            return CollectionUtil.newHashMap(OAuth2TokenIntrospectionClaimNames.ACTIVE, false);
        }

        // 校验授权记录对应的客户端是否仍已注册
        RegisteredClient authorizedClient = this.registeredClientRepository.findById(
                authorization.getRegisteredClientId());
        if(ObjectUtil.isNullEmpty(authorizedClient)){
            return CollectionUtil.newHashMap(OAuth2TokenIntrospectionClaimNames.ACTIVE, false);
        }

        // 构建内省响应
        return getClaims(authorization, authorizedClient, accessToken);

    }

    /**
     * 构建内省响应声明集合，合并令牌原有 claims 并移除 iss 声明
     *
     * @param authorization 授权记录，提供 sub 声明
     * @param authorizedClient 令牌对应的已注册客户端，提供 client_id 声明
     * @param accessToken 授权记录中有效的访问令牌，提供 token_type 与原有 claims
     * @return 内省响应声明集合，始终非空
     */
    @Nonnull
    private static Map<String, Object> getClaims(OAuth2Authorization authorization,
            RegisteredClient authorizedClient, OAuth2Authorization.Token<OAuth2AccessToken> accessToken) {

        Map<String, Object> claims = CollectionUtil.newHashMap();

        claims.put(OAuth2TokenIntrospectionClaimNames.ACTIVE, true);
        claims.put(OAuth2TokenIntrospectionClaimNames.SUB, authorization.getPrincipalName());
        claims.put(OAuth2TokenIntrospectionClaimNames.CLIENT_ID, authorizedClient.getId());
        claims.put(OAuth2TokenIntrospectionClaimNames.TOKEN_TYPE, accessToken.getToken().getTokenType().getValue());

        // 合并令牌原有claims，并移除iss（issuer）声明
        Map<String, Object> oldClaims = accessToken.getClaims();
        if (oldClaims != null) {
            claims.putAll(oldClaims);
            claims.remove(OAuth2TokenIntrospectionClaimNames.ISS);
        }

        return claims;

    }

}
