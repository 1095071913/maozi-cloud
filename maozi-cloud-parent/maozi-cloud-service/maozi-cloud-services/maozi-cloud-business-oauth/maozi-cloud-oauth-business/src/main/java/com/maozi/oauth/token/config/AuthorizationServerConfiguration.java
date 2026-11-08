
/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package com.maozi.oauth.token.config;

import cn.hutool.extra.spring.SpringUtil;
import com.maozi.oauth.token.config.authorization.password.PasswordAuthenticationConverter;
import com.maozi.oauth.token.config.authorization.password.PasswordAuthenticationProvider;
import com.maozi.oauth.token.config.authorization.refresh.RefreshTokenAuthenticationProvider;
import com.maozi.oauth.token.config.handle.ClientAuthenticationEntryPoint;
import com.maozi.oauth.token.config.handle.ClientAuthenticationFailureHandler;
import com.maozi.oauth.token.config.handle.TokenRevocationSuccessHandler;
import com.maozi.oauth.token.config.handle.UserAuthenticationFailureHandler;
import com.maozi.oauth.token.config.handle.UserAuthenticationSuccessHandler;
import lombok.SneakyThrows;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2RefreshTokenAuthenticationProvider;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.security.oauth2.server.authorization.web.authentication.DelegatingAuthenticationConverter;
import org.springframework.security.oauth2.server.authorization.web.authentication.OAuth2AuthorizationCodeAuthenticationConverter;
import org.springframework.security.oauth2.server.authorization.web.authentication.OAuth2AuthorizationCodeRequestAuthenticationConverter;
import org.springframework.security.oauth2.server.authorization.web.authentication.OAuth2ClientCredentialsAuthenticationConverter;
import org.springframework.security.oauth2.server.authorization.web.authentication.OAuth2RefreshTokenAuthenticationConverter;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationConverter;

import java.util.Arrays;

/**
 * OAuth2授权服务器配置类
 * <p>
 * 配置Spring Authorization Server的核心组件，包括安全过滤器链、
 * 各种认证模式（密码模式、刷新令牌模式等）、令牌端点处理器、
 * 密码编码器以及授权确认服务等。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
@Configuration
public class AuthorizationServerConfiguration {

	/**
	 * 配置授权服务器的安全过滤器链
	 * <p>
	 * 设置OAuth2授权服务器的默认安全配置，包括OIDC端点、客户端认证失败处理、
	 * 令牌撤销端点、令牌端点（用户认证成功/失败处理、登录参数转换器、刷新Token处理器）等。
	 * </p>
	 *
	 * @param http HttpSecurity对象，用于构建安全过滤器链
	 * @return SecurityFilterChain 授权服务器的安全过滤器链
	 * @throws Exception 配置过程中可能抛出的异常
	 */
	@Bean
	public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {

		OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);

		http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)

			//开启端点
			.oidc(Customizer.withDefaults())

			//客户端认证失败
			.clientAuthentication(client -> client
				.errorResponseHandler(new ClientAuthenticationFailureHandler())
			)

			.tokenRevocationEndpoint(tokenRevocationEndpoint -> tokenRevocationEndpoint
				//销毁令牌成功响应
				.revocationResponseHandler(new TokenRevocationSuccessHandler())
				//自定义撤销处理器：撤销令牌后物理删除对应授权记录
				.authenticationProvider(SpringUtil.getBean(TokenRevocationAuthenticationProvider.class))
			)

			.tokenEndpoint(tokenEndpoint -> tokenEndpoint
				//用户认证授权成功响应
				.accessTokenResponseHandler(new UserAuthenticationSuccessHandler())
				//注册各种登陆参数转换器
				.accessTokenRequestConverter(accessTokenRequestConverter())
				//用户认证授权失败
				.errorResponseHandler(new UserAuthenticationFailureHandler())
				//替换默认的刷新Token处理器，添加权限信息
				.authenticationProviders(authenticationProviders -> {
					authenticationProviders.removeIf(provider -> provider instanceof OAuth2RefreshTokenAuthenticationProvider);
					RefreshTokenAuthenticationProvider refreshProvider = SpringUtil.getBean(RefreshTokenAuthenticationProvider.class);
					refreshProvider.setTokenGenerator(http.getSharedObject(OAuth2TokenGenerator.class));
					authenticationProviders.add(refreshProvider);
				})
			)

		;

		//客户端未认证授权
		http.exceptionHandling(ex -> ex.authenticationEntryPoint(new ClientAuthenticationEntryPoint()));

		DefaultSecurityFilterChain securityFilterChain = http.build();

		//注册各种认证模式
		addCustomOAuth2GrantAuthenticationProvider(http);

		return securityFilterChain;

	}

	/**
	 * 注册各种登陆参数校验转换器
	 *
	 * @return DelegatingAuthenticationConverter 委托式认证转换器，聚合密码模式、刷新令牌、
	 *         客户端凭证、授权码及授权码请求等转换器
	 */
	private AuthenticationConverter accessTokenRequestConverter() {
		return new DelegatingAuthenticationConverter(
			Arrays.asList(
				new PasswordAuthenticationConverter(),
				new OAuth2RefreshTokenAuthenticationConverter(),
				new OAuth2ClientCredentialsAuthenticationConverter(),
				new OAuth2AuthorizationCodeAuthenticationConverter(),
				new OAuth2AuthorizationCodeRequestAuthenticationConverter()
			)
		);
	}

	/**
	 * 注册自定义登陆模式的认证处理器（当前仅密码模式）
	 *
	 * @param http HttpSecurity对象，用于注册认证处理器并获取共享的令牌生成器
	 */
	private void addCustomOAuth2GrantAuthenticationProvider(HttpSecurity http){

		OAuth2TokenGenerator<?> tokenGenerator = http.getSharedObject(OAuth2TokenGenerator.class);

		//密码模式
		PasswordAuthenticationProvider passwordProvider = SpringUtil.getBean(PasswordAuthenticationProvider.class);
		passwordProvider.setTokenGenerator(tokenGenerator);
		http.authenticationProvider(passwordProvider);

	}



	/**
	 * 将AuthenticationManager注入ioc中，其它需要使用地方可以直接从ioc中获取
	 *
	 * @param authenticationConfiguration 导出认证配置
	 * @return AuthenticationManager 认证管理器
	 */
	@Bean
	@SneakyThrows
	public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) {
		return authenticationConfiguration.getAuthenticationManager();
	}

//	@Bean
//	public OAuth2AuthorizationService authorizationService(JdbcTemplate jdbcTemplate, RegisteredClientRepository registeredClientRepository) {
//		return new JdbcOAuth2AuthorizationService(jdbcTemplate, registeredClientRepository);
//	}

	/**
	 * 配置基于db的授权确认管理服务
	 *
	 * @param jdbcTemplate               db数据源信息
	 * @param registeredClientRepository 客户端repository
	 * @return JdbcOAuth2AuthorizationConsentService
	 */
	@Bean
	public OAuth2AuthorizationConsentService authorizationConsentService(JdbcTemplate jdbcTemplate, RegisteredClientRepository registeredClientRepository) {
		return new JdbcOAuth2AuthorizationConsentService(jdbcTemplate, registeredClientRepository);
	}

	/**
	 * 配置密码编码器
	 * <p>
	 * 使用委托式密码编码器（DelegatingPasswordEncoder），默认使用BCrypt算法进行密码匹配。
	 * 支持多种密码编码格式的兼容。
	 * </p>
	 *
	 * @return PasswordEncoder 密码编码器实例
	 */
	@Bean
	public PasswordEncoder passwordEncoder() {
		DelegatingPasswordEncoder passwordEncoder = (DelegatingPasswordEncoder) PasswordEncoderFactories.createDelegatingPasswordEncoder();
		passwordEncoder.setDefaultPasswordEncoderForMatches(new BCryptPasswordEncoder());
		return passwordEncoder;
	}

}