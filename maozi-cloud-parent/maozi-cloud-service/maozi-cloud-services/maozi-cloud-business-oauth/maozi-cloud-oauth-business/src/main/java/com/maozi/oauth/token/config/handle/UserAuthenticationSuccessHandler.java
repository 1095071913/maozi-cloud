package com.maozi.oauth.token.config.handle;

import com.maozi.common.ResultUtil;
import com.maozi.common.WebUtil;
import com.maozi.oauth.token.dto.OauthTokenDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;

import java.time.Duration;

/**
 * 用户认证成功处理器
 * <p>
 * 当用户认证授权成功后，将OAuth2令牌信息（AccessToken、RefreshToken、IDToken等）
 * 封装为OauthTokenDTO对象，并以统一格式返回给客户端。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Slf4j
public class UserAuthenticationSuccessHandler implements org.springframework.security.web.authentication.AuthenticationSuccessHandler {

	/**
	 * 处理用户认证成功事件
	 * <p>
	 * 从认证结果中提取AccessToken、RefreshToken、IDToken等信息，
	 * 计算令牌有效期时长（单位秒），封装为OauthTokenDTO对象后写入响应。
	 * </p>
	 *
	 * @param request       HTTP请求对象
	 * @param response      HTTP响应对象
	 * @param authentication 认证结果（OAuth2AccessTokenAuthenticationToken）
	 */
	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,Authentication authentication) {

		OAuth2AccessTokenAuthenticationToken accessTokenAuthentication = (OAuth2AccessTokenAuthenticationToken) authentication;
		OAuth2AccessToken accessToken = accessTokenAuthentication.getAccessToken();

		OauthTokenDTO oauthToken = new OauthTokenDTO();

		oauthToken.setTokenType(accessToken.getTokenType().getValue());
		oauthToken.setAccessToken(accessToken.getTokenValue());

		if (accessToken.getIssuedAt() != null && accessToken.getExpiresAt() != null) {
			long expiresIn = Duration.between(accessToken.getIssuedAt(), accessToken.getExpiresAt()).getSeconds();
			oauthToken.setExpiresIn(expiresIn);
		}

		OAuth2RefreshToken refreshToken = accessTokenAuthentication.getRefreshToken();
		if (refreshToken != null) {

			oauthToken.setRefreshToken(refreshToken.getTokenValue());

			if (refreshToken.getIssuedAt() != null && refreshToken.getExpiresAt() != null) {
				oauthToken.setRefreshTokenExpiresIn(Duration.between(refreshToken.getIssuedAt(), refreshToken.getExpiresAt()).getSeconds());
			}

		}

		oauthToken.setScope(String.join(" ",accessToken.getScopes()));

		Object idToken = accessTokenAuthentication.getAdditionalParameters().get("id_token");
		if (idToken != null) {
			oauthToken.setIdToken(idToken.toString());
		}

		WebUtil.writeResponseBody(response, ResultUtil.success(oauthToken));

	}

}