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

package com.maozi.oauth.client.param;

import com.maozi.base.enums.Status;
import com.maozi.oauth.client.enums.AuthType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;
import java.util.Set;


/**
 * 客户端保存/更新参数
 * <p>
 * 用于OAuth客户端新增和更新操作的请求参数，包含客户端的基本配置信息、
 * 授权模式、令牌有效期等核心配置项。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientSaveUpdateParam implements Serializable {
    /** 序列化标识 */
    @Serial
    private static final long serialVersionUID = 1L;

	/** 客户端ID，新增时由系统自动生成，更新时会被清空以防被修改，API接口中隐藏 */
	@Schema(hidden = true)
	private String clientId;

	/** 客户端密钥，用于客户端认证，不能为空 */
	@NotEmpty(message = "客户端密钥不能为空")
	@Schema(description = "客户端密钥")
	private String clientSecret;

	/** 客户端名称，用于标识和描述客户端，不能为空 */
	@NotEmpty(message = "名称不能为空")
	@Schema(description = "名称")
	private String name;

	/** 授权模式集合，指定该客户端支持的OAuth2.0授权类型 */
	@Schema(description = "授权模式",ref = "IntegerArrayList")
	private Set<AuthType> authorizationGrantTypes;

	/** 授权令牌（AccessToken）有效期，单位为秒，新增时未填默认2小时 */
	@Schema(description = "授权令牌有效期 秒 默认2小时")
	private Long accessTokenValiditySeconds;

	/** 刷新令牌（RefreshToken）有效期，单位为秒，新增时未填默认7天 */
	@Schema(description = "刷新令牌有效期 秒 默认7天")
	private Long refreshTokenValiditySeconds;

	/** 备注信息，用于记录客户端的额外说明 */
	@Schema(description = "备注")
	private String remark;

	/** 客户端状态，控制客户端是否启用 */
	@Schema(description = "状态",ref = "int")
	private Status status;

	/** 客户端配置项，存储OAuth2.0客户端的自定义配置，API接口中隐藏 */
	@Schema(hidden = true)
	private Map<String,Object> clientSettings;

	/** 令牌配置项，存储OAuth2.0令牌相关的自定义配置，API接口中隐藏 */
	@Schema(hidden = true)
	private Map<String,Object> tokenSettings;
	
}
