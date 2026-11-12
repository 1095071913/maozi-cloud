package com.maozi.oauth.token.config;

import org.springframework.lang.Nullable;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2TokenRevocationAuthenticationProvider;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2TokenRevocationAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

/**
 * 令牌撤销认证提供者
 * <p>
 * 包装默认的令牌撤销处理器（OAuth2TokenRevocationAuthenticationProvider），
 * 在令牌成功撤销后，从授权服务中物理删除匹配的授权记录，
 * 确保令牌被彻底清除而不仅仅是标记为失效。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Service
public final class TokenRevocationAuthenticationProvider implements AuthenticationProvider {

	/** 委托的默认令牌撤销认证处理器 */
	private final OAuth2TokenRevocationAuthenticationProvider delegate;

	/** OAuth2授权信息服务，用于管理授权记录的存储和查找 */
	private final OAuth2AuthorizationService authorizationService;

	/**
	 * 构造函数，初始化令牌撤销认证提供者
	 *
	 * @param authorizationService OAuth2授权信息服务，不能为空
	 */
	public TokenRevocationAuthenticationProvider(OAuth2AuthorizationService authorizationService) {
		Assert.notNull(authorizationService, "authorizationService cannot be null");
		this.authorizationService = authorizationService;
		this.delegate = new OAuth2TokenRevocationAuthenticationProvider(authorizationService);
	}

	/**
	 * 执行令牌撤销认证
	 * <p>
	 * 先委托给默认处理器执行撤销操作，然后根据撤销请求中的令牌信息
	 * 查找并物理删除对应的授权记录。
	 * </p>
	 *
	 * @param authentication 包含撤销令牌信息的认证对象
	 * @return 认证结果
	 */
	@Override
	public Authentication authenticate(Authentication authentication) {
		Authentication result = this.delegate.authenticate(authentication);
		if (authentication instanceof OAuth2TokenRevocationAuthenticationToken tokenRequest) {
			OAuth2TokenType tokenType = resolveTokenType(tokenRequest.getTokenTypeHint());
			OAuth2Authorization authorization = this.authorizationService.findByToken(tokenRequest.getToken(), tokenType);
			if (authorization != null) {
				this.authorizationService.remove(authorization);
			}
		}
		return result;
	}

	/**
	 * 判断是否支持该认证类型
	 *
	 * @param authentication 认证类型的Class对象
	 * @return 如果是OAuth2TokenRevocationAuthenticationToken类型则返回true
	 */
	@Override
	public boolean supports(Class<?> authentication) {
		return OAuth2TokenRevocationAuthenticationToken.class.isAssignableFrom(authentication);
	}

	/**
	 * 根据令牌类型提示解析为OAuth2TokenType对象
	 * <p>
	 * 支持解析的令牌类型包括：access_token、refresh_token、code、state、user_code、device_code、id_token。
	 * </p>
	 *
	 * @param tokenTypeHint 令牌类型提示字符串
	 * @return 对应的OAuth2TokenType对象，如果无法识别则返回null
	 */
	@Nullable
	private OAuth2TokenType resolveTokenType(@Nullable String tokenTypeHint) {
		if (!org.springframework.util.StringUtils.hasText(tokenTypeHint)) {
			return null;
		}
		if (OAuth2ParameterNames.ACCESS_TOKEN.equals(tokenTypeHint)) {
			return OAuth2TokenType.ACCESS_TOKEN;
		}
		if (OAuth2ParameterNames.REFRESH_TOKEN.equals(tokenTypeHint)) {
			return OAuth2TokenType.REFRESH_TOKEN;
		}
		if (OAuth2ParameterNames.CODE.equals(tokenTypeHint)) {
			return new OAuth2TokenType(OAuth2ParameterNames.CODE);
		}
		if (OAuth2ParameterNames.STATE.equals(tokenTypeHint)) {
			return new OAuth2TokenType(OAuth2ParameterNames.STATE);
		}
		if (OAuth2ParameterNames.USER_CODE.equals(tokenTypeHint)) {
			return new OAuth2TokenType(OAuth2ParameterNames.USER_CODE);
		}
		if (OAuth2ParameterNames.DEVICE_CODE.equals(tokenTypeHint)) {
			return new OAuth2TokenType(OAuth2ParameterNames.DEVICE_CODE);
		}
		if (OidcParameterNames.ID_TOKEN.equals(tokenTypeHint)) {
			return new OAuth2TokenType(OidcParameterNames.ID_TOKEN);
		}
		return null;
	}
}
