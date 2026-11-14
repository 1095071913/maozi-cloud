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

package com.maozi.system.user.api.rpc;

import com.maozi.common.result.AbstractBaseResult;
import com.maozi.system.user.result.AiGetUserInfoResult;
import com.maozi.system.user.result.OauthUserInfoResult;

/**
 * 用户服务 RPC 接口
 * <p>
 * 供其他微服务内部调用，包括 OAuth 认证用户信息查询与 AI 工具用户信息查询。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
public interface RpcUserService {

	/**
	 * 根据用户名获取 OAuth 认证所需的用户信息
	 * <p>
	 * 供授权服务器进行密码模式认证与权限填充。
	 * </p>
	 *
	 * @param username 用户名
	 * @return OAuth 认证用户信息（含用户ID、密码、权限标识列表）
	 */
	AbstractBaseResult<OauthUserInfoResult> getOauthUserInfoByUsernameResult(String username);

	/**
	 * 根据用户 ID 获取 AI 工具所需的用户信息
	 *
	 * @param userId 用户 ID，用于查询指定用户的信息
	 * @return AI 查询用户信息结果（含用户ID、客户端、名称、权限列表等）
	 */
	AbstractBaseResult<AiGetUserInfoResult> aiGetUserInfoResult(Long userId);

}
