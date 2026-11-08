/*
 * Copyright 2020-2023 the original author or authors.
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
 */
package com.maozi.oauth.token.config.service.impl;


import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.maozi.oauth.token.config.service.OAuth2AuthorizationService;
import com.maozi.oauth.token.param.ClientUserParam;
import com.maozi.redis.utils.RedisUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2DeviceCode;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.core.OAuth2UserCode;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.jackson2.OAuth2AuthorizationServerJackson2Module;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 基于Redis的OAuth2授权信息服务实现
 * <p>
 * 将OAuth2授权信息序列化后存入Redis，模拟JDBC服务行为：authorization:{id} 键存储完整授权数据，
 * authorization:index:{tokenType}:{tokenValue} 键作为state和令牌值的辅助查找索引，
 * authorization:principal:{clientId}:{principalName} 键作为用户主体索引（以上键均带
 * {@code RedisUtil.REDIS_KEY_PREFIX} 前缀）。支持按ID或按令牌（可选令牌类型）查找，
 * 并实现{@link OAuth2AuthorizationService}扩展的按用户单个/批量注销授权能力。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */

@Service
public class RedisOAuth2AuthorizationServiceImpl implements OAuth2AuthorizationService {

	/** 授权数据的Redis键前缀 */
	private static final String AUTHORIZATION_KEY_PREFIX = RedisUtil.REDIS_KEY_PREFIX + "authorization:";

	/** 令牌索引的Redis键前缀 */
	private static final String INDEX_KEY_PREFIX = RedisUtil.REDIS_KEY_PREFIX + "authorization:index:";

	/** 用户主体索引的Redis键前缀，用于按客户端和用户名查找授权 */
	private static final String PRINCIPAL_INDEX_KEY_PREFIX = RedisUtil.REDIS_KEY_PREFIX + "authorization:principal:";

	/** state类型的标识常量 */
	private static final String STATE_TYPE = "state";

	/** JSON序列化/反序列化对象映射器 */
	private final ObjectMapper objectMapper;

	/** Redis字符串操作模板 */
	private final StringRedisTemplate redisTemplate;

	/** 已注册客户端仓库，用于重建授权对象时查找客户端信息 */
	private final RegisteredClientRepository registeredClientRepository;

	/**
	 * 构造函数
	 *
	 * @param redisTemplate             Redis字符串操作模板
	 * @param registeredClientRepository 已注册客户端仓库
	 */
	public RedisOAuth2AuthorizationServiceImpl(StringRedisTemplate redisTemplate, RegisteredClientRepository registeredClientRepository) {
		this.objectMapper = createObjectMapper();
		this.redisTemplate = redisTemplate;
		this.registeredClientRepository = registeredClientRepository;
	}

	/**
	 * 创建配置了安全模块的ObjectMapper实例
	 * <p>
	 * 注册Spring Security的Jackson模块和OAuth2授权服务器的Jackson模块，
	 * 以支持OAuth2相关对象的正确序列化和反序列化。
	 * </p>
	 *
	 * @return 配置好的ObjectMapper实例
	 */
	private static ObjectMapper createObjectMapper() {
		ObjectMapper objectMapper = new ObjectMapper();
		ClassLoader classLoader = RedisOAuth2AuthorizationServiceImpl.class.getClassLoader();
		List<Module> securityModules = SecurityJackson2Modules.getModules(classLoader);
		objectMapper.registerModules(securityModules);
		objectMapper.registerModule(new OAuth2AuthorizationServerJackson2Module());
		objectMapper.setDefaultTyping(trustedDefaultTyping());
		return objectMapper;
	}

	/**
	 * 构建受信任的 default typing，覆盖 SecurityJackson2Modules 内置的严格类白名单。
	 * <p>
	 * 授权数据由服务端自行序列化写入 Redis（受信任来源），其 attributes 和 token claims 以
	 * {@code Map<String, Object>} 形式存储，default typing 会对声明类型为 {@code Object} 的值
	 * 强制写入类型信息（@class），即便实际值是 {@link Long}、{@link String} 等 final 类型。
	 * SecurityJackson2Modules 内置的 AllowlistTypeIdResolver 仅放行少数预定义类，对 JDK 包装类型
	 * 及业务对象会抛出 IllegalArgumentException。由于数据来源可信，这里按 Spring Security 官方
	 * 建议启用放开式的 default typing（{@code allowIfSubType(Object.class)}）彻底放行所有类型，
	 * 安全性依赖 Redis 不被外部篡改。
	 * </p>
	 *
	 * @return 配置好的 TypeResolverBuilder 实例
	 */
	private static TypeResolverBuilder<?> trustedDefaultTyping() {
		PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
				.allowIfSubType(Object.class)
				.build();
		return new ObjectMapper.DefaultTypeResolverBuilder(ObjectMapper.DefaultTyping.NON_FINAL, ptv)
				.init(JsonTypeInfo.Id.CLASS, null)
				.inclusion(JsonTypeInfo.As.PROPERTY);
	}

	/**
	 * 保存OAuth2授权信息到Redis
	 * <p>
	 * 将授权数据序列化后存储到Redis，同时创建令牌索引和用户主体索引。
	 * 保存前会先清理同一客户端下同一用户的旧授权记录，确保每个用户每个客户端只保留最新的授权。
	 * </p>
	 *
	 * @param authorization OAuth2授权信息，不能为空
	 */
	@Override
	public void save(OAuth2Authorization authorization) {
		Assert.notNull(authorization, "authorization cannot be null");
		AuthorizationData data = AuthorizationData.from(authorization);
		String authKey = authorizationKey(authorization.getId());
		String payload = write(data);

		removeExistingAuthorizations(data.registeredClientId(), data.principalName(), authorization.getId());

		OAuth2Authorization previous = findById(authorization.getId());
		if (previous != null) {
			remove(previous);
		}

		Duration authorizationTtl = resolveAuthorizationTtl(data);
		setValue(authKey, payload, authorizationTtl);

		for (TokenIndex index : data.indexes()) {
			String indexKey = indexKey(index.type(), index.value());
			Duration ttl = resolveIndexTtl(data, index.type(), index.value());
			setValue(indexKey, authorization.getId(), ttl);
		}

		String principalIndexKey = principalIndexKey(data.registeredClientId(), data.principalName());
		this.redisTemplate.opsForSet().add(principalIndexKey, authorization.getId());
		setTtl(principalIndexKey, authorizationTtl);
	}

	/**
	 * 从Redis中删除OAuth2授权信息
	 * <p>
	 * 删除授权数据主键及所有关联的令牌索引键，同时从用户主体索引中移除该授权ID。
	 * </p>
	 *
	 * @param authorization 要删除的OAuth2授权信息，不能为空
	 */
	@Override
	public void remove(OAuth2Authorization authorization) {
		Assert.notNull(authorization, "authorization cannot be null");
		OAuth2Authorization current = findById(authorization.getId());
		if (current == null) {
			return;
		}
		AuthorizationData data = AuthorizationData.from(current);
		List<String> keys = new ArrayList<>();
		keys.add(authorizationKey(authorization.getId()));
		for (TokenIndex index : data.indexes()) {
			keys.add(indexKey(index.type(), index.value()));
		}
		this.redisTemplate.delete(keys);
		this.redisTemplate.opsForSet().remove(principalIndexKey(data.registeredClientId(), data.principalName()), authorization.getId());
	}

	/**
	 * 根据授权ID查找OAuth2授权信息
	 *
	 * @param id 授权ID，不能为空
	 * @return 匹配的OAuth2授权信息，如果不存在则返回null
	 */
	@Nullable
	@Override
	public OAuth2Authorization findById(String id) {
		Assert.hasText(id, "id cannot be empty");
		String payload = this.redisTemplate.opsForValue().get(authorizationKey(id));
		if (!StringUtils.hasText(payload)) {
			return null;
		}
		return rebuild(read(payload));
	}

	/**
	 * 根据令牌值和令牌类型查找OAuth2授权信息
	 * <p>
	 * 如果未指定令牌类型，则按所有已知令牌类型依次查找。
	 * 支持的令牌类型包括：state、authorization_code、access_token、
	 * oidc_id_token、refresh_token、user_code、device_code。
	 * </p>
	 *
	 * @param token     令牌值，不能为空
	 * @param tokenType 令牌类型，可为null（表示按所有类型查找）
	 * @return 匹配的OAuth2授权信息，如果不存在则返回null
	 */
	@Nullable
	@Override
	public OAuth2Authorization findByToken(String token, @Nullable OAuth2TokenType tokenType) {
		Assert.hasText(token, "token cannot be empty");
		if (tokenType == null) {
			return findByTokenIndex(STATE_TYPE, token,
					"authorization_code", "access_token", "oidc_id_token",
					"refresh_token", "user_code", "device_code");
		}

		if (OAuth2ParameterNames.STATE.equals(tokenType.getValue())) {
			return findByTokenIndex(STATE_TYPE, token);
		}
		if (OAuth2ParameterNames.CODE.equals(tokenType.getValue())) {
			return findByTokenIndex("authorization_code", token);
		}
		if (OAuth2TokenType.ACCESS_TOKEN.equals(tokenType)) {
			return findByTokenIndex("access_token", token);
		}
		if (OidcParameterNames.ID_TOKEN.equals(tokenType.getValue())) {
			return findByTokenIndex("oidc_id_token", token);
		}
		if (OAuth2TokenType.REFRESH_TOKEN.equals(tokenType)) {
			return findByTokenIndex("refresh_token", token);
		}
		if (OAuth2ParameterNames.USER_CODE.equals(tokenType.getValue())) {
			return findByTokenIndex("user_code", token);
		}
		if (OAuth2ParameterNames.DEVICE_CODE.equals(tokenType.getValue())) {
			return findByTokenIndex("device_code", token);
		}
		return null;
	}

	/**
	 * 通过令牌索引查找授权信息
	 * <p>
	 * 按照指定的令牌类型依次查找，如果第一种类型未找到，则尝试后续的备用类型。
	 * </p>
	 *
	 * @param type          首选令牌类型
	 * @param token         令牌值
	 * @param fallbackTypes 备用的令牌类型列表
	 * @return 匹配的OAuth2授权信息，如果全部未找到则返回null
	 */
	@Nullable
	private OAuth2Authorization findByTokenIndex(String type, String token, String... fallbackTypes) {
		List<String> types = new ArrayList<>();
		types.add(type);
		Collections.addAll(types, fallbackTypes);
		for (String candidateType : types) {
			String id = this.redisTemplate.opsForValue().get(indexKey(candidateType, token));
			if (StringUtils.hasText(id)) {
				return findById(id);
			}
		}
		return null;
	}

	/**
	 * 从授权数据重建OAuth2Authorization对象
	 * <p>
	 * 根据Redis中存储的授权数据，查找对应的已注册客户端，
	 * 重建包括授权码、AccessToken、IDToken、RefreshToken、UserCode、DeviceCode在内的完整授权对象。
	 * </p>
	 *
	 * @param data Redis中存储的授权数据
	 * @return 重建的OAuth2Authorization对象，如果客户端不存在则返回null
	 */
	private OAuth2Authorization rebuild(AuthorizationData data) {
		RegisteredClient registeredClient = this.registeredClientRepository.findById(data.registeredClientId());
		if (registeredClient == null) {
			return null;
		}

		OAuth2Authorization.Builder builder = OAuth2Authorization.withRegisteredClient(registeredClient);
		builder.id(data.id())
				.principalName(data.principalName())
				.authorizationGrantType(new AuthorizationGrantType(data.authorizationGrantType()))
				.authorizedScopes(data.authorizedScopes())
				.attributes(attrs -> attrs.putAll(data.attributes()));

		if (StringUtils.hasText(data.state())) {
			builder.attribute(OAuth2ParameterNames.STATE, data.state());
		}

		applyToken(builder, data.authorizationCode());
		applyToken(builder, data.accessToken());
		applyToken(builder, data.oidcIdToken());
		applyToken(builder, data.refreshToken());
		applyToken(builder, data.userCode());
		applyToken(builder, data.deviceCode());
		return builder.build();
	}

	/**
	 * 将令牌数据应用到OAuth2Authorization构建器中
	 * <p>
	 * 根据令牌类型（授权码、AccessToken、IDToken、RefreshToken、UserCode、DeviceCode）
	 * 创建对应的令牌对象并设置到构建器中。
	 * </p>
	 *
	 * @param builder   OAuth2Authorization构建器
	 * @param tokenData 令牌数据，包含令牌类型、值、签发时间、过期时间和元数据
	 * @param <T>       OAuth2令牌类型
	 */
	private <T extends OAuth2Token> void applyToken(OAuth2Authorization.Builder builder, TokenData<T> tokenData) {
		if (tokenData == null || tokenData.value() == null) {
			return;
		}
		if (tokenData.type() == TokenKind.AUTHORIZATION_CODE) {
			OAuth2AuthorizationCode token = new OAuth2AuthorizationCode(tokenData.value(), tokenData.issuedAt(), tokenData.expiresAt());
			builder.token(token, metadata -> metadata.putAll(tokenData.metadata()));
			return;
		}
		if (tokenData.type() == TokenKind.ACCESS_TOKEN) {
			OAuth2AccessToken.TokenType tokenType = OAuth2AccessToken.TokenType.BEARER;
			Set<String> scopes = tokenData.scopes() != null ? tokenData.scopes() : Collections.emptySet();
			OAuth2AccessToken token = new OAuth2AccessToken(tokenType, tokenData.value(), tokenData.issuedAt(), tokenData.expiresAt(), scopes);
			builder.token(token, metadata -> metadata.putAll(tokenData.metadata()));
			return;
		}
		if (tokenData.type() == TokenKind.OIDC_ID_TOKEN) {
			@SuppressWarnings("unchecked")
			Map<String, Object> claims = (Map<String, Object>) tokenData.metadata()
					.getOrDefault(OAuth2Authorization.Token.CLAIMS_METADATA_NAME, Collections.emptyMap());
			OidcIdToken token = new OidcIdToken(tokenData.value(), tokenData.issuedAt(), tokenData.expiresAt(), claims);
			builder.token(token, metadata -> metadata.putAll(tokenData.metadata()));
			return;
		}
		if (tokenData.type() == TokenKind.REFRESH_TOKEN) {
			OAuth2RefreshToken token = new OAuth2RefreshToken(tokenData.value(), tokenData.issuedAt(), tokenData.expiresAt());
			builder.token(token, metadata -> metadata.putAll(tokenData.metadata()));
			return;
		}
		if (tokenData.type() == TokenKind.USER_CODE) {
			OAuth2UserCode token = new OAuth2UserCode(tokenData.value(), tokenData.issuedAt(), tokenData.expiresAt());
			builder.token(token, metadata -> metadata.putAll(tokenData.metadata()));
			return;
		}
		if (tokenData.type() == TokenKind.DEVICE_CODE) {
			OAuth2DeviceCode token = new OAuth2DeviceCode(tokenData.value(), tokenData.issuedAt(), tokenData.expiresAt());
			builder.token(token, metadata -> metadata.putAll(tokenData.metadata()));
		}
	}

	/**
	 * 设置Redis键值对，支持可选的过期时间
	 *
	 * @param key   Redis键
	 * @param value Redis值
	 * @param ttl   过期时间，如果为null或非正数则不设置过期时间
	 */
	private void setValue(String key, String value, @Nullable Duration ttl) {
		if (ttl != null && !ttl.isNegative() && !ttl.isZero()) {
			this.redisTemplate.opsForValue().set(key, value, ttl);
			return;
		}
		this.redisTemplate.opsForValue().set(key, value);
	}

	/**
	 * 为Redis键设置过期时间
	 *
	 * @param key Redis键
	 * @param ttl 过期时间，如果为null或非正数则不设置过期时间
	 */
	private void setTtl(String key, @Nullable Duration ttl) {
		if (ttl != null && !ttl.isNegative() && !ttl.isZero()) {
			this.redisTemplate.expire(key, ttl);
		}
	}

	/**
	 * 解析授权数据的TTL（存活时间）
	 * <p>
	 * 根据所有令牌中最晚的过期时间计算授权数据的整体TTL。
	 * </p>
	 *
	 * @param data 授权数据
	 * @return 授权数据的TTL，如果没有任何令牌有过期时间则返回null，若最晚过期时间已过则返回零时长
	 */
	private Duration resolveAuthorizationTtl(AuthorizationData data) {
		Instant maxExpiry = null;
		for (TokenData<?> token : data.tokens()) {
			if (token != null && token.expiresAt() != null) {
				if (maxExpiry == null || token.expiresAt().isAfter(maxExpiry)) {
					maxExpiry = token.expiresAt();
				}
			}
		}
		if (maxExpiry == null) {
			return null;
		}
		Duration ttl = Duration.between(Instant.now(), maxExpiry);
		return ttl.isNegative() ? Duration.ZERO : ttl;
	}

	/**
	 * 解析令牌索引的TTL（存活时间）
	 * <p>
	 * 根据令牌类型找到对应的令牌数据，计算从当前时间到令牌过期时间的TTL。
	 * </p>
	 *
	 * @param data  授权数据
	 * @param type  令牌类型
	 * @param value 令牌值
	 * @return 令牌索引的TTL，如果令牌不存在或无过期时间则返回null，若令牌已过期则返回零时长
	 */
	private Duration resolveIndexTtl(AuthorizationData data, String type, String value) {
		TokenData<?> token = switch (type) {
			case "authorization_code" -> data.authorizationCode();
			case "access_token" -> data.accessToken();
			case "oidc_id_token" -> data.oidcIdToken();
			case "refresh_token" -> data.refreshToken();
			case "user_code" -> data.userCode();
			case "device_code" -> data.deviceCode();
			case STATE_TYPE -> null;
			default -> null;
		};
		if (token == null || !Objects.equals(token.value(), value)) {
			return null;
		}
		if (token.expiresAt() == null) {
			return null;
		}
		Duration ttl = Duration.between(Instant.now(), token.expiresAt());
		return ttl.isNegative() ? Duration.ZERO : ttl;
	}

	/**
	 * 生成授权数据的Redis键
	 *
	 * @param id 授权ID
	 * @return 授权数据的Redis键
	 */
	private String authorizationKey(String id) {
		return AUTHORIZATION_KEY_PREFIX + id;
	}

	/**
	 * 生成令牌索引的Redis键
	 *
	 * @param type  令牌类型
	 * @param value 令牌值
	 * @return 令牌索引的Redis键（令牌值经过Base64编码）
	 */
	private String indexKey(String type, String value) {
		return INDEX_KEY_PREFIX + type + ":" + encode(value);
	}

	/**
	 * 生成用户主体索引的Redis键
	 *
	 * @param registeredClientId 已注册客户端ID
	 * @param principalName      用户主体名称
	 * @return 用户主体索引的Redis键（各部分经过Base64编码）
	 */
	private String principalIndexKey(String registeredClientId, String principalName) {
		return PRINCIPAL_INDEX_KEY_PREFIX + encode(registeredClientId) + ":" + encode(principalName);
	}

	/**
	 * 移除同一客户端下同一用户的所有授权记录
	 * <p>
	 * 通过用户主体索引查找该用户在该客户端下的所有授权ID，
	 * 移除除当前授权ID以外的所有旧授权记录。
	 * </p>
	 *
	 * @param registeredClientId    已注册客户端ID
	 * @param principalName         用户主体名称
	 * @param currentAuthorizationId 当前要保留的授权ID
	 */
	private void removeExistingAuthorizations(String registeredClientId, String principalName, String currentAuthorizationId) {
		Set<String> ids = this.redisTemplate.opsForSet().members(principalIndexKey(registeredClientId, principalName));
		if (ids == null || ids.isEmpty()) {
			return;
		}
		for (String id : ids) {
			if (!Objects.equals(id, currentAuthorizationId)) {
				OAuth2Authorization existing = findById(id);
				if (existing != null) {
					remove(existing);
				}
			}
		}
	}

	/**
	 * 移除指定客户端下指定用户的所有授权记录（注销）
	 * <p>
	 * 通过用户主体索引查找该用户在该客户端下的所有授权ID，
	 * 并逐一移除，实现用户注销功能。
	 * </p>
	 *
	 * @param registeredClientId 已注册客户端ID
	 * @param principalName      用户主体名称（用户名）
	 */
	@Override
	public void removeAllByPrincipal(Long registeredClientId, String principalName) {
		Set<String> ids = this.redisTemplate.opsForSet().members(principalIndexKey(registeredClientId.toString(), principalName));
		if (ids == null || ids.isEmpty()) {
			return;
		}
		for (String id : ids) {
			OAuth2Authorization existing = findById(id);
			if (existing != null) {
				remove(existing);
			}
		}
	}

	/**
	 * 批量移除多个用户的所有授权记录（批量注销）
	 * <p>
	 * 通过MGET一次性获取所有授权数据，解析出所有需要删除的键（授权主键 + 令牌索引键 + 主体索引键），
	 * 最终通过一次批量DEL完成删除，适用于角色禁用等需要同时注销多个用户的场景。
	 * </p>
	 *
	 * @param clientUsers 客户端用户参数列表，每项包含clientId和username
	 */
	@Override
	public void removeAllByPrincipals(List<ClientUserParam> clientUsers) {
		if (clientUsers == null || clientUsers.isEmpty()) {
			return;
		}

		// 1. 根据每个用户的clientId和username计算主体索引键，通过SMEMBERS获取每个用户在该客户端下的所有授权ID
		List<String> principalIndexKeys = new ArrayList<>();
		Set<String> allAuthIds = new HashSet<>();
		for (ClientUserParam clientUser : clientUsers) {
			String pKey = principalIndexKey(clientUser.getClientId().toString(), clientUser.getUsername());
			principalIndexKeys.add(pKey);
			Set<String> ids = this.redisTemplate.opsForSet().members(pKey);
			if (ids != null) {
				allAuthIds.addAll(ids);
			}
		}

		// 所有用户均无授权记录，仅删除主体索引键后返回
		if (allAuthIds.isEmpty()) {
			this.redisTemplate.delete(principalIndexKeys);
			return;
		}

		// 2. 根据授权ID计算授权主键，通过MGET一次性获取所有授权数据的JSON字符串
		List<String> authKeys = allAuthIds.stream().map(this::authorizationKey).toList();
		List<String> payloads = this.redisTemplate.opsForValue().multiGet(authKeys);

		// 3. 反序列化授权数据，提取令牌索引键（access_token、refresh_token等），与授权主键、主体索引键合并为待删除列表
		List<String> keysToDelete = new ArrayList<>(principalIndexKeys);
		keysToDelete.addAll(authKeys);
		if (payloads != null) {
			for (String payload : payloads) {
				if (StringUtils.hasText(payload)) {
					AuthorizationData data = read(payload);
					for (TokenIndex index : data.indexes()) {
						keysToDelete.add(indexKey(index.type(), index.value()));
					}
				}
			}
		}

		// 4. 一次批量DEL删除所有Redis键，完成注销
		this.redisTemplate.delete(keysToDelete);

	}

	/**
	 * 将JSON字符串反序列化为AuthorizationData对象
	 *
	 * @param payload JSON格式的授权数据字符串
	 * @return 反序列化后的AuthorizationData对象
	 * @throws IllegalArgumentException 反序列化失败时抛出
	 */
	private AuthorizationData read(String payload) {
		try {
			return this.objectMapper.readValue(payload, AuthorizationData.class);
		} catch (Exception ex) {
			throw new IllegalArgumentException(ex.getMessage(), ex);
		}
	}

	/**
	 * 将AuthorizationData对象序列化为JSON字符串
	 *
	 * @param data 授权数据对象
	 * @return JSON格式的字符串
	 * @throws IllegalArgumentException 序列化失败时抛出
	 */
	private String write(AuthorizationData data) {
		try {
			return this.objectMapper.writeValueAsString(data);
		} catch (Exception ex) {
			throw new IllegalArgumentException(ex.getMessage(), ex);
		}
	}

	/**
	 * 使用Base64 URL安全编码对字符串进行编码（无填充）
	 *
	 * @param value 要编码的字符串
	 * @return 编码后的字符串
	 */
	private static String encode(String value) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
	}

	/** 令牌类型枚举，用于标识不同种类的OAuth2令牌 */
	private enum TokenKind {
		/** 授权码 */
		AUTHORIZATION_CODE,
		/** 访问令牌 */
		ACCESS_TOKEN,
		/** OIDC ID令牌 */
		OIDC_ID_TOKEN,
		/** 刷新令牌 */
		REFRESH_TOKEN,
		/** 用户码（设备授权流程） */
		USER_CODE,
		/** 设备码（设备授权流程） */
		DEVICE_CODE
	}

	/** 令牌索引记录，包含令牌类型和令牌值 */
	private record TokenIndex(String type, String value) {}

	/** 令牌数据记录，包含令牌的完整信息 */
	private record TokenData<T extends OAuth2Token>(TokenKind type, String value,
	                                                @Nullable Instant issuedAt, @Nullable Instant expiresAt,
	                                                Map<String, Object> metadata, @Nullable Set<String> scopes) {
	}

	/**
	 * 授权数据记录，用于Redis中OAuth2Authorization的序列化存储
	 * <p>
	 * 包含授权ID、客户端ID、用户主体名称、授权类型、授权范围、属性、
	 * state以及各类令牌（授权码、AccessToken、IDToken、RefreshToken、UserCode、DeviceCode）的数据。
	 * </p>
	 */
	private record AuthorizationData(String id, String registeredClientId, String principalName,
	                                 String authorizationGrantType, Set<String> authorizedScopes,
	                                 Map<String, Object> attributes, @Nullable String state,
	                                 @Nullable TokenData<OAuth2AuthorizationCode> authorizationCode,
	                                 @Nullable TokenData<OAuth2AccessToken> accessToken,
	                                 @Nullable TokenData<OidcIdToken> oidcIdToken,
	                                 @Nullable TokenData<OAuth2RefreshToken> refreshToken,
	                                 @Nullable TokenData<OAuth2UserCode> userCode,
	                                 @Nullable TokenData<OAuth2DeviceCode> deviceCode) {

		/**
		 * 从OAuth2Authorization对象创建AuthorizationData记录
		 *
		 * @param authorization OAuth2授权对象
		 * @return 转换后的AuthorizationData记录
		 */
		static AuthorizationData from(OAuth2Authorization authorization) {
			Map<String, Object> attributes = new HashMap<>(authorization.getAttributes());
			String state = authorization.getAttribute(OAuth2ParameterNames.STATE);
			return new AuthorizationData(
					authorization.getId(),
					authorization.getRegisteredClientId(),
					authorization.getPrincipalName(),
					authorization.getAuthorizationGrantType().getValue(),
					authorization.getAuthorizedScopes() != null ? new HashSet<>(authorization.getAuthorizedScopes()) : Collections.emptySet(),
					attributes,
					state,
					toTokenData(TokenKind.AUTHORIZATION_CODE, authorization.getToken(OAuth2AuthorizationCode.class)),
					toTokenData(TokenKind.ACCESS_TOKEN, authorization.getToken(OAuth2AccessToken.class)),
					toTokenData(TokenKind.OIDC_ID_TOKEN, authorization.getToken(OidcIdToken.class)),
					toTokenData(TokenKind.REFRESH_TOKEN, authorization.getRefreshToken()),
					toTokenData(TokenKind.USER_CODE, authorization.getToken(OAuth2UserCode.class)),
					toTokenData(TokenKind.DEVICE_CODE, authorization.getToken(OAuth2DeviceCode.class)));
		}

		/**
		 * 获取所有需要建立索引的令牌索引列表
		 * <p>
		 * 包括state、授权码、AccessToken、IDToken、RefreshToken、UserCode、DeviceCode。
		 * </p>
		 *
		 * @return 令牌索引列表
		 */
		List<TokenIndex> indexes() {
			List<TokenIndex> indexes = new ArrayList<>();
			if (StringUtils.hasText(this.state)) {
				indexes.add(new TokenIndex(STATE_TYPE, this.state));
			}
			addIndex(indexes, "authorization_code", this.authorizationCode);
			addIndex(indexes, "access_token", this.accessToken);
			addIndex(indexes, "oidc_id_token", this.oidcIdToken);
			addIndex(indexes, "refresh_token", this.refreshToken);
			addIndex(indexes, "user_code", this.userCode);
			addIndex(indexes, "device_code", this.deviceCode);
			return indexes;
		}

		/**
		 * 获取所有非空的令牌数据列表
		 *
		 * @return 令牌数据列表
		 */
		List<TokenData<?>> tokens() {
			List<TokenData<?>> tokens = new ArrayList<>();
			if (this.authorizationCode != null) tokens.add(this.authorizationCode);
			if (this.accessToken != null) tokens.add(this.accessToken);
			if (this.oidcIdToken != null) tokens.add(this.oidcIdToken);
			if (this.refreshToken != null) tokens.add(this.refreshToken);
			if (this.userCode != null) tokens.add(this.userCode);
			if (this.deviceCode != null) tokens.add(this.deviceCode);
			return tokens;
		}

		/**
		 * 如果令牌数据不为空且令牌值有效，则添加到索引列表中
		 *
		 * @param indexes 索引列表
		 * @param type    令牌类型标识
		 * @param token   令牌数据
		 */
		private static void addIndex(List<TokenIndex> indexes, String type, @Nullable TokenData<?> token) {
			if (token != null && StringUtils.hasText(token.value())) {
				indexes.add(new TokenIndex(type, token.value()));
			}
		}

		/**
		 * 将OAuth2Authorization.Token转换为TokenData记录
		 *
		 * @param kind  令牌类型枚举
		 * @param token OAuth2授权令牌，可为null
		 * @param <T>   OAuth2令牌类型
		 * @return 转换后的TokenData记录，如果输入token为null则返回null
		 */
		private static <T extends OAuth2Token> TokenData<T> toTokenData(TokenKind kind, @Nullable OAuth2Authorization.Token<T> token) {
			if (token == null) {
				return null;
			}
			Set<String> scopes = null;
			if (token.getToken() instanceof OAuth2AccessToken accessToken) {
				scopes = accessToken.getScopes();
			}
			return new TokenData<>(kind,
					token.getToken().getTokenValue(),
					token.getToken().getIssuedAt(),
					token.getToken().getExpiresAt(),
					new HashMap<>(token.getMetadata()),
					scopes != null ? new HashSet<>(scopes) : null);
		}
	}
}
