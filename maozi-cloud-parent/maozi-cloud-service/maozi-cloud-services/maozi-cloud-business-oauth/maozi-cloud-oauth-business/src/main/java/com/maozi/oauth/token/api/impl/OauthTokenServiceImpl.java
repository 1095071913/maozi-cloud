package com.maozi.oauth.token.api.impl;

import com.maozi.common.CollectionUtil;
import com.maozi.common.ObjectUtil;
import com.maozi.oauth.token.api.OauthTokenService;
import com.maozi.oauth.token.config.service.OAuth2AuthorizationService;
import com.maozi.oauth.token.constants.OAuth2TokenClaimConstants;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Resource;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * OAuth 令牌服务本地实现
 * <p>
 * 实现资源服务器对访问令牌（Access Token）的内省逻辑：基于
 * {@link OAuth2AuthorizationService#findByToken} 查询授权记录，
 * 校验令牌是否仍处于有效状态，并按需返回
 * active、sub、client_id、token_type、authorities、scope 等声明。
 * 该本地实现由 {@code RpcOauthTokenServiceImpl} 通过 Dubbo RPC、
 * {@code RestOauthTokenServiceImpl} 通过 REST 接口对外暴露，
 * 资源服务器经 Dubbo 或 HTTP 调用即可完成令牌校验。
 * </p>
 *
 * @author pengjinlong
 * @since 2026/6/24 23:58
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
     * {@inheritDoc}
     * <p>
     * 实现要点：
     * <ul>
     *   <li>先按 access_token 维度查找授权记录，未命中直接返回 active=false；</li>
     *   <li>命中后再校验 AccessToken 是否仍处于 active 状态；</li>
     *   <li>授权记录对应的客户端必须仍已在客户端仓库中注册，否则同样返回
     *   active=false；</li>
     *   <li>合并令牌原有的全部 claims，并移除 iss（issuer）声明。</li>
     * </ul>
     * </p>
     */
    @Override
    public Map<String, Object> introspect(String token) {

        // 通过access_token查找对应的授权记录
        OAuth2Authorization authorization = authorizationService.findByToken(token, OAuth2TokenType.ACCESS_TOKEN);
        if (authorization == null) {
            return CollectionUtil.newHashMap(OAuth2TokenClaimConstants.ACTIVE, false);
        }

        // 校验授权记录中的AccessToken是否有效
        OAuth2Authorization.Token<OAuth2AccessToken> accessToken = authorization.getAccessToken();
        if (accessToken == null || !accessToken.isActive()) {
            return CollectionUtil.newHashMap(OAuth2TokenClaimConstants.ACTIVE, false);
        }

        // 校验授权记录对应的客户端是否仍已注册，未注册视为令牌无效
        RegisteredClient authorizedClient = this.registeredClientRepository.findById(
                authorization.getRegisteredClientId());
        if(ObjectUtil.isNullEmpty(authorizedClient)){
            return CollectionUtil.newHashMap(OAuth2TokenClaimConstants.ACTIVE, false);
        }

        // 构建内省响应，填充令牌基本声明字段

        return getClaims(authorization, authorizedClient, accessToken);

    }

    /**
     * 构建令牌内省响应的声明集合
     * <p>
     * 填充 active、sub、client_id、token_type 基本声明后，合并访问令牌原有
     * claims 并移除 iss（issuer）声明
     * </p>
     *
     * @param authorization 授权记录，提供 sub（principalName）声明，不可为空
     * @param authorizedClient 令牌对应的已注册客户端，提供 client_id 声明，不可为空
     * @param accessToken 授权记录中有效的访问令牌，提供 token_type 与原有 claims
     * @return 内省响应声明集合，始终非空
     */
    @Nonnull
    private static Map<String, Object> getClaims(OAuth2Authorization authorization,
            RegisteredClient authorizedClient, OAuth2Authorization.Token<OAuth2AccessToken> accessToken) {

        Map<String, Object> claims = CollectionUtil.newHashMap();

        claims.put(OAuth2TokenClaimConstants.ACTIVE, true);
        claims.put(OAuth2TokenClaimConstants.SUB, authorization.getPrincipalName());
        claims.put(OAuth2TokenClaimConstants.CLIENT_ID, authorizedClient.getId());
        claims.put(OAuth2TokenClaimConstants.TOKEN_TYPE, accessToken.getToken().getTokenType().getValue());

        // 合并令牌原有claims，并移除iss（issuer）声明
        Map<String, Object> oldClaims = accessToken.getClaims();
        if (oldClaims != null) {
            claims.putAll(oldClaims);
            claims.remove(OAuth2TokenClaimConstants.ISS);
        }

        return claims;

    }

}
