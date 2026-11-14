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

package com.maozi.oauth.client.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.maozi.db.domain.AbstractBaseDomain;
import com.maozi.db.handler.CommaSeparatedTypeHandler;
import com.maozi.oauth.client.enums.AuthType;
import com.maozi.oauth.client.handler.OAuth2SettingsTypeHandler;
import com.maozi.oauth.client.handler.Oauth2AuthTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.util.Map;
import java.util.Set;

/**
 * OAuth2客户端实体类。
 * <p>
 * 对应数据库表 oauth2_registered_client，存储OAuth2注册客户端的完整信息；
 * 继承 AbstractBaseDomain 获得id、状态等公共字段。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
@TableName(value = "oauth2_registered_client",autoResultMap = true)
public class ClientDO extends AbstractBaseDomain {
	/** 序列化标识 */
	@Serial
	private static final long serialVersionUID = 1L;

	/** 客户端标识，客户端的唯一ID，由系统自动生成 */
	private String clientId;

	/** 客户端名称，用于标识和描述客户端，映射到数据库的client_name字段 */
	@TableField("client_name")
	private String name;

	/** 客户端密钥，用于客户端认证，存储时已加密 */
	private String clientSecret;

	/** 备注信息，用于记录客户端的额外说明 */
	private String remark;

	/** 授权方式集合，该客户端支持的OAuth2.0授权类型，通过自定义类型处理器与数据库逗号分隔字符串互转 */
	@TableField(typeHandler = Oauth2AuthTypeHandler.class)
	private Set<AuthType> authorizationGrantTypes;

	/** 授权范围集合，定义客户端可以请求的权限范围，以逗号分隔存储 */
	@TableField(typeHandler = CommaSeparatedTypeHandler.class)
	private Set<String> scopes;

	/** 重定向地址集合，授权码模式中用于接收授权码的回调地址，以逗号分隔存储 */
	@TableField(typeHandler = CommaSeparatedTypeHandler.class)
	private Set<String> redirectUris;

	/** 客户端配置项，存储OAuth2.0客户端的自定义配置（如是否需要用户确认授权等） */
	@TableField(typeHandler = OAuth2SettingsTypeHandler.class)
	private Map<String,Object> clientSettings;

	/** 令牌配置项，存储OAuth2.0令牌相关的自定义配置（如令牌有效期、格式等） */
	@TableField(typeHandler = OAuth2SettingsTypeHandler.class)
	private Map<String,Object> tokenSettings;
	
}
