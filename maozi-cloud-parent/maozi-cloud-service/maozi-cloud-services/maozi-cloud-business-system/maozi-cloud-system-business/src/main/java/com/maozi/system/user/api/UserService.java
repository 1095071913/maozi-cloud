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

package com.maozi.system.user.api;


import com.maozi.oauth.token.param.ClientUserParam;

import java.util.List;

/**
 * 用户服务接口
 * <p>
 * 提供用户相关的公共服务方法定义，包括根据用户 ID 获取客户端用户信息、
 * 根据用户 ID 列表批量获取客户端用户信息等功能。该接口为用户模块的核心服务接口，
 * 由 UserServiceImpl 实现具体业务逻辑。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
public interface UserService {

	/**
	 * 根据用户ID获取客户端用户信息
	 *
	 * @param id 用户ID
	 * @return 客户端用户信息参数对象，包含用户名和客户端ID
	 */
	ClientUserParam getClientUser(Long id);

	/**
	 * 根据用户ID列表批量获取客户端用户信息
	 *
	 * @param ids 用户ID列表
	 * @return 客户端用户信息参数对象列表
	 */
	List<ClientUserParam> getClientUsers(List<Long> ids);

}
