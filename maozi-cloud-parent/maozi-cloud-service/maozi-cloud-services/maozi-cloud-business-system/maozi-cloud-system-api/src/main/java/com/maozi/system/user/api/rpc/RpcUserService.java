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

import java.util.List;

/**
 * 用户服务 RPC 接口
 * <p>
 * 提供用户相关的 RPC 远程调用接口定义，
 * 主要用于微服务之间的内部调用，包括根据用户名获取 OAuth 认证用户信息
 * （含用户ID、加密密码及全部权限标识）、根据用户名和角色 ID
 * 获取该角色下的权限标识列表，以及供 AI 工具根据用户 ID
 * 查询用户信息等功能。
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
	AbstractBaseResult<OauthUserInfoResult> rpcGetOauthUserInfoByUsername(String username);

	/**
	 * 根据用户名和角色 ID 获取权限列表
	 * <p>
	 * 查询指定用户名在指定角色下所拥有的权限编码列表，
	 * 用于特定角色场景下的权限校验。
	 * </p>
	 *
	 * @param username 用户名，用于查询该用户的信息
	 * @param roleId   角色 ID，用于限定查询的角色范围
	 * @return 返回该用户在指定角色下拥有的权限编码列表
	 */
	AbstractBaseResult<List<String>> rpcGetPermissionsByUsernameRole(String username,Long roleId);

	/**
	 * 根据用户 ID 获取 AI 工具所需的用户信息
	 * <p>
	 * 返回内容包括用户ID、所属客户端、用户名称、客户端信息
	 * 以及该用户拥有的权限列表，供 AI 工具查询用户信息时使用。
	 * </p>
	 *
	 * @param userId 用户 ID，用于查询指定用户的信息
	 * @return AI 查询用户信息结果（含用户ID、客户端、名称、权限列表等）
	 */
	AbstractBaseResult<AiGetUserInfoResult> rpcAiGetUserInfo(Long userId);

}
