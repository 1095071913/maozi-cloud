package com.maozi.oauth.token.config.authorization.refresh;

import com.maozi.oauth.token.config.domain.SecurityUserDetails;
import com.maozi.oauth.token.constants.OAuth2TokenClaimConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2RefreshTokenAuthenticationProvider;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2RefreshTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 自定义刷新Token认证提供者
 * <p>
 * 包装默认的 {@link OAuth2RefreshTokenAuthenticationProvider}，在刷新Token后
 * 将用户权限与附加属性（如用户ID）写入新AccessToken的claims metadata中，
 * 确保刷新后的Token通过introspection端点能正确返回权限信息。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenAuthenticationProvider implements AuthenticationProvider {

	/** OAuth2授权信息服务，用于管理授权记录 */
	private final OAuth2AuthorizationService authorizationService;

	/** 用户详情服务，用于加载用户权限信息 */
	private final UserDetailsService userDetailsService;

	/** 委托的默认刷新Token处理器 */
	private OAuth2RefreshTokenAuthenticationProvider delegate;

	/**
	 * 设置令牌生成器，同时初始化委托的默认刷新Token处理器
	 *
	 * @param tokenGenerator OAuth2令牌生成器，不能为空
	 */
	public void setTokenGenerator(OAuth2TokenGenerator<?> tokenGenerator) {
		Assert.notNull(tokenGenerator, "tokenGenerator cannot be null");
		this.delegate = new OAuth2RefreshTokenAuthenticationProvider(this.authorizationService, tokenGenerator);
	}

	/**
	 * 执行刷新Token认证
	 * <p>
	 * 委托给默认的刷新Token处理器执行刷新操作，
	 * 然后将用户权限与附加属性（如用户ID）写入新生成的AccessToken的claims metadata中。
	 * </p>
	 *
	 * @param authentication 刷新Token认证请求
	 * @return 认证结果（包含新AccessToken的认证令牌）
	 * @throws AuthenticationException 认证过程中出现的异常
	 */
	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		Assert.notNull(this.delegate, "tokenGenerator must be set via setTokenGenerator()");

		// 委托给默认的刷新Token处理器
		Authentication result = this.delegate.authenticate(authentication);

		if (result instanceof OAuth2AccessTokenAuthenticationToken accessTokenAuth) {
			enrichAuthorizationWithAuthorities(accessTokenAuth.getAccessToken());
		}

		return result;
	}

	/**
	 * 将用户权限与附加属性写入新AccessToken的claims metadata，与密码模式保持一致
	 *
	 * @param accessToken 刷新后新生成的访问令牌
	 */
	private void enrichAuthorizationWithAuthorities(OAuth2AccessToken accessToken) {
		OAuth2Authorization authorization = authorizationService.findByToken(
				accessToken.getTokenValue(), OAuth2TokenType.ACCESS_TOKEN);

		if (authorization == null) {
			return;
		}

		// 重新加载用户当前权限
		UserDetails userDetails = userDetailsService.loadUserByUsername(authorization.getPrincipalName());
		List<String> authorities = userDetails.getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
				.collect(Collectors.toList());

		// 获取已有claims并添加authorities
		OAuth2Authorization.Token<OAuth2AccessToken> accessTokenToken = authorization.getAccessToken();
		Map<String, Object> existingClaims = accessTokenToken != null && accessTokenToken.getClaims() != null
				? accessTokenToken.getClaims()
				: Collections.emptyMap();
		Map<String, Object> claims = new HashMap<>(existingClaims);
		claims.put(OAuth2TokenClaimConstants.AUTHORITIES, authorities);
		if(userDetails instanceof SecurityUserDetails securityUserDetails){
			claims.putAll(securityUserDetails.getAttributes());
		}

		// 重建授权信息，写入authorities到claims metadata
		OAuth2Authorization.Builder builder = OAuth2Authorization.from(authorization);
		builder.token(accessToken, metadata ->
				metadata.put(OAuth2Authorization.Token.CLAIMS_METADATA_NAME, claims));

		authorizationService.save(builder.build());

		if (log.isTraceEnabled()) {
			log.trace("Refreshed access token enriched with {} authorities for user: {}",
					authorities.size(), authorization.getPrincipalName());
		}
	}

	/**
	 * 判断是否支持该认证类型
	 *
	 * @param authentication 认证类型的Class对象
	 * @return 如果是OAuth2RefreshTokenAuthenticationToken类型则返回true
	 */
	@Override
	public boolean supports(Class<?> authentication) {
		return OAuth2RefreshTokenAuthenticationToken.class.isAssignableFrom(authentication);
	}

}
