package com.maozi.oauth.token.config.authorization.base;

import com.baomidou.mybatisplus.core.toolkit.ReflectionKit;
import com.maozi.oauth.token.config.domain.SecurityUserDetails;
import com.maozi.oauth.token.constants.OAuth2TokenUserClaimConstants;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClaimAccessor;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder;
import org.springframework.security.oauth2.server.authorization.token.DefaultOAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import java.security.Principal;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 自定义授权模式认证提供者基类
 * <p>
 * 抽象基类，为各种自定义授权模式（如密码模式、短信验证码模式等）提供
 * 统一的认证处理流程，包括客户端认证验证、scope验证、用户认证、
 * AccessToken/RefreshToken/IDToken的生成以及授权信息的保存。
 * 子类只需实现subAuthenticate方法提供具体的用户认证逻辑即可。
 * </p>
 *
 * @param <T> 自定义授权令牌类型，必须继承自BaseGrantAuthenticationToken
 * @author maozi
 * @date 2026/11/08
 */
@Slf4j
public abstract class BaseGrantAuthenticationProvider<T extends BaseGrantAuthenticationToken> implements AuthenticationProvider {

    /** OAuth2令牌生成器，用于生成各类令牌 */
    private OAuth2TokenGenerator<?> tokenGenerator;

    /** OAuth2授权信息服务，用于管理授权记录的存储、查找和删除 */
    @Resource
    private OAuth2AuthorizationService authorizationService;

    /** 基础DAO认证提供者，用于执行基于数据库的用户名密码认证 */
    @Resource
    private BaseDaoAuthenticationProvider daoAuthenticationProvider;

    /** OAuth2错误参考文档URI */
    private static final String ERROR_URI = "https://datatracker.ietf.org/doc/html/rfc6749#section-5.2";

    /** ID Token的令牌类型标识 */
    private static final OAuth2TokenType ID_TOKEN_TOKEN_TYPE = new OAuth2TokenType(OidcParameterNames.ID_TOKEN);

    /** 当前泛型对应的Token令牌Class对象，用于判断支持的认证类型 */
    protected Class<T> tokenClass = currentTokenClass();

    /**
     * 通过反射获取当前子类的泛型Token类型
     *
     * @return 泛型Token的Class对象
     */
    protected Class<T> currentTokenClass() {
        return (Class<T>) ReflectionKit.getSuperClassGenericType(this.getClass(), BaseGrantAuthenticationProvider.class, 0);
    }

    /**
     * 判断是否支持该认证类型
     *
     * @param authentication 认证类型的Class对象
     * @return 如果是当前泛型Token类型则返回true
     */
    @Override
    public boolean supports(Class<?> authentication) {
        return tokenClass.isAssignableFrom(authentication);
    }

    /**
     * 子类认证方法，由子类实现具体的用户认证逻辑
     *
     * @param authentication 自定义授权令牌
     * @return 包含用户名和密码的用户名密码认证令牌
     */
    public abstract UsernamePasswordAuthenticationToken subAuthenticate(T authentication);

    /**
     * 执行认证流程：校验客户端、授权模式与scope后，委托子类完成用户认证，
     * 再生成AccessToken（claims中写入用户权限与用户附加属性）、可选的RefreshToken与IDToken（scope含openid时），
     * 最后保存授权信息并返回
     *
     * @param authentication 认证请求对象
     * @return 认证结果（OAuth2AccessTokenAuthenticationToken）
     * @throws AuthenticationException 认证过程中出现的异常
     */
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {

        T authenticationToken = (T)authentication;

        // 确保客户端已通过认证
        OAuth2ClientAuthenticationToken clientPrincipal = getAuthenticatedClientElseThrowInvalidClient(authenticationToken);
        RegisteredClient registeredClient = clientPrincipal.getRegisteredClient();
        // 确保客户端已配置该授权模式
        if (registeredClient == null || !registeredClient.getAuthorizationGrantTypes().contains(authenticationToken.getAuthorizationGrantType())) {
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.UNAUTHORIZED_CLIENT);
        }

        // 验证scope
        Set<String> authorizedScopes = getAuthorizedScopes(registeredClient, authenticationToken.getScopes());

        // 调用子类认证逻辑构造用户名密码令牌，再由DAO认证提供者完成密码校验
        Authentication userAuthenticationToken = null;
        try {
            UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = subAuthenticate(authenticationToken);
            userAuthenticationToken = daoAuthenticationProvider.authenticate(usernamePasswordAuthenticationToken);
        } catch (Exception e) {
            // 认证异常统一转为OAuth2认证异常返回
            throw new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST, e.getMessage(), ERROR_URI));
        }

        // 以下令牌生成流程参考OAuth2AuthorizationCodeAuthenticationProvider实现
        DefaultOAuth2TokenContext.Builder tokenContextBuilder = DefaultOAuth2TokenContext.builder()
                .registeredClient(registeredClient)
                .principal(userAuthenticationToken)
                .authorizationServerContext(AuthorizationServerContextHolder.getContext())
                .authorizedScopes(authorizedScopes)
                .authorizationGrantType(authenticationToken.getAuthorizationGrantType())
                .authorizationGrant(authenticationToken);

        // 初始化OAuth2Authorization构建器
        OAuth2Authorization.Builder authorizationBuilder = OAuth2Authorization.withRegisteredClient(registeredClient)
                // 加入当前用户认证信息，防止刷新token时因获取不到认证信息而抛出空指针异常
                // 存入授权scope
                .authorizedScopes(authorizedScopes)
                // 当前授权用户名称
                .principalName(userAuthenticationToken.getName())
                // 设置当前用户认证信息
                .attribute(Principal.class.getName(), userAuthenticationToken)
                .authorizationGrantType(authenticationToken.getAuthorizationGrantType());

        // ----- Access token -----
        OAuth2TokenContext tokenContext = tokenContextBuilder.tokenType(OAuth2TokenType.ACCESS_TOKEN).build();
        OAuth2Token generatedAccessToken = this.tokenGenerator.generate(tokenContext);
        if (generatedAccessToken == null) {
            OAuth2Error error = new OAuth2Error(OAuth2ErrorCodes.SERVER_ERROR,
                    "The token generator failed to generate the access token.", ERROR_URI);
            throw new OAuth2AuthenticationException(error);
        }

        if (log.isTraceEnabled()) {
            log.trace("Generated access token");
        }
        OAuth2AccessToken accessToken = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER,
                generatedAccessToken.getTokenValue(), generatedAccessToken.getIssuedAt(),
                generatedAccessToken.getExpiresAt(), tokenContext.getAuthorizedScopes());
        // JWT类型令牌：在生成器产出的claims基础上补充用户权限与用户附加属性
        if (generatedAccessToken instanceof ClaimAccessor) {
            Authentication finalUserAuthenticationToken = userAuthenticationToken;
            authorizationBuilder.token(accessToken, (metadata) -> {
                Map<String, Object> claims = new HashMap<>(((ClaimAccessor) generatedAccessToken).getClaims());
                List<String> authorities = finalUserAuthenticationToken.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toList());
                claims.put(OAuth2TokenUserClaimConstants.AUTHORITIES, authorities);
                if(finalUserAuthenticationToken.getPrincipal() instanceof SecurityUserDetails securityUserDetails){
                    claims.putAll(securityUserDetails.getAttributes());
                }
                metadata.put(OAuth2Authorization.Token.CLAIMS_METADATA_NAME, claims);
            });
        } else {
            // 不透明令牌：直接将用户权限与用户附加属性构建为claims metadata
            Map<String, Object> claims = new HashMap<>();
            List<String> authorities = userAuthenticationToken.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());
            claims.put(OAuth2TokenUserClaimConstants.AUTHORITIES, authorities);
            if(userAuthenticationToken.getPrincipal() instanceof SecurityUserDetails securityUserDetails){
                claims.putAll(securityUserDetails.getAttributes());
            }
            authorizationBuilder.token(accessToken, (metadata) ->
                    metadata.put(OAuth2Authorization.Token.CLAIMS_METADATA_NAME, claims));
        }
        // ----- Refresh token -----
        OAuth2RefreshToken refreshToken = null;
        if (registeredClient.getAuthorizationGrantTypes().contains(AuthorizationGrantType.REFRESH_TOKEN) &&
                // 不向公共客户端颁发refresh token
                !clientPrincipal.getClientAuthenticationMethod().equals(ClientAuthenticationMethod.NONE)) {

            tokenContext = tokenContextBuilder.tokenType(OAuth2TokenType.REFRESH_TOKEN).build();
            OAuth2Token generatedRefreshToken = this.tokenGenerator.generate(tokenContext);
            if (!(generatedRefreshToken instanceof OAuth2RefreshToken)) {
                OAuth2Error error = new OAuth2Error(OAuth2ErrorCodes.SERVER_ERROR,
                        "The token generator failed to generate the refresh token.", ERROR_URI);
                throw new OAuth2AuthenticationException(error);
            }

            if (log.isTraceEnabled()) {
                log.trace("Generated refresh token");
            }
            refreshToken = (OAuth2RefreshToken) generatedRefreshToken;
            authorizationBuilder.refreshToken(refreshToken);
        }

        // ----- ID token -----
        OidcIdToken idToken;
        if (authorizedScopes.contains(OidcScopes.OPENID)) {
            tokenContext = tokenContextBuilder
                    .tokenType(ID_TOKEN_TOKEN_TYPE)
                    // ID token定制器可能需要访问access token或refresh token
                    .authorization(authorizationBuilder.build())
                    .build();
            // @formatter:on
            OAuth2Token generatedIdToken = this.tokenGenerator.generate(tokenContext);
            if (!(generatedIdToken instanceof Jwt)) {
                OAuth2Error error = new OAuth2Error(OAuth2ErrorCodes.SERVER_ERROR,
                        "The token generator failed to generate the ID token.", ERROR_URI);
                throw new OAuth2AuthenticationException(error);
            }

            if (log.isTraceEnabled()) {
                log.trace("Generated id token");
            }

            idToken = new OidcIdToken(generatedIdToken.getTokenValue(), generatedIdToken.getIssuedAt(),
                    generatedIdToken.getExpiresAt(), ((Jwt) generatedIdToken).getClaims());
            authorizationBuilder.token(idToken, (metadata) ->
                    metadata.put(OAuth2Authorization.Token.CLAIMS_METADATA_NAME, idToken.getClaims()));
        } else {
            idToken = null;
        }

        OAuth2Authorization authorization = authorizationBuilder.build();

        // 保存授权信息
        this.authorizationService.save(authorization);

        Map<String, Object> additionalParameters = new HashMap<>(1);
        if (idToken != null) {
            // 放入idToken
            additionalParameters.put(OidcParameterNames.ID_TOKEN, idToken.getTokenValue());
        }

        return new OAuth2AccessTokenAuthenticationToken(registeredClient, clientPrincipal, accessToken, refreshToken, additionalParameters);
    }

    /**
     * 校验请求的scope并确定最终授权的scope
     * <p>
     * 请求scope为空时默认使用客户端配置的scope，否则逐一校验请求scope
     * 是否在客户端配置范围内，超出范围则抛出INVALID_REQUEST异常
     * </p>
     *
     * @param registeredClient 已注册客户端，提供允许的scope范围
     * @param requestedScopes  请求中的scope，可为null或空
     * @return 校验通过的最终授权scope集合
     */
    private Set<String> getAuthorizedScopes(RegisteredClient registeredClient, Set<String> requestedScopes) {
        // 默认使用客户端配置的scope
        Set<String> authorizedScopes = registeredClient.getScopes();
        if (!ObjectUtils.isEmpty(requestedScopes)) {
            Set<String> unauthorizedScopes = requestedScopes.stream()
                    .filter(requestedScope -> !registeredClient.getScopes().contains(requestedScope))
                    .collect(Collectors.toSet());
            if (!ObjectUtils.isEmpty(unauthorizedScopes)) {
                throw new OAuth2AuthenticationException(new OAuth2Error( OAuth2ErrorCodes.INVALID_REQUEST, "OAuth 2.0 Parameter: " + OAuth2ParameterNames.SCOPE, ERROR_URI));
            }

            authorizedScopes = new LinkedHashSet<>(requestedScopes);
        }

        if (log.isTraceEnabled()) {
            log.trace("Validated token request parameters");
        }
        return authorizedScopes;
    }

    /**
     * 设置OAuth2令牌生成器
     *
     * @param tokenGenerator 令牌生成器实例，不能为空
     */
    public void setTokenGenerator(OAuth2TokenGenerator<?> tokenGenerator) {
        Assert.notNull(tokenGenerator, "tokenGenerator cannot be null");
        this.tokenGenerator = tokenGenerator;
    }

    /**
     * 获取已认证的客户端信息，如果客户端未认证则抛出无效客户端异常
     *
     * @param authentication 认证对象
     * @return 已认证的客户端认证令牌
     * @throws OAuth2AuthenticationException 当客户端未认证时抛出INVALID_CLIENT错误
     */
    public static OAuth2ClientAuthenticationToken getAuthenticatedClientElseThrowInvalidClient(Authentication authentication) {
        OAuth2ClientAuthenticationToken clientPrincipal = null;
        if (OAuth2ClientAuthenticationToken.class.isAssignableFrom(authentication.getPrincipal().getClass())) {
            clientPrincipal = (OAuth2ClientAuthenticationToken) authentication.getPrincipal();
        }
        if (clientPrincipal != null && clientPrincipal.isAuthenticated()) {
            return clientPrincipal;
        }
        throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_CLIENT);
    }

}
