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

package com.maozi.oauth.token.config.service.impl;

import com.maozi.common.CollectionUtil;
import com.maozi.common.result.error.code.SystemErrorCode;
import com.maozi.common.result.error.exception.BusinessResultException;
import com.maozi.oauth.token.config.domain.SecurityUserDetails;
import com.maozi.oauth.token.constants.OAuth2TokenUserClaimConstants;
import com.maozi.service.api.annotation.RemoteResource;
import com.maozi.system.user.api.rpc.RpcUserService;
import com.maozi.system.user.result.OauthUserInfoResult;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 用户详情服务实现类
 * <p>
 * 实现Spring Security的UserDetailsService接口，通过Dubbo RPC远程调用用户服务
 * 获取用户的权限列表和密码信息，构建并返回Spring Security的UserDetails对象。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

	/** 用户服务RPC接口，通过Dubbo远程调用获取用户信息 */
	@RemoteResource
	private RpcUserService rpcUserService;

	/**
	 * 根据用户名加载用户详情，通过RPC获取用户权限与加密密码，并将用户ID写入附加属性（随令牌claims一并颁发）
	 *
	 * @param username 用户名
	 * @return 包含用户名、密码和权限的UserDetails对象
	 * @throws BusinessResultException 业务错误（如用户不存在）抛出USER_AUTH_ERROR，其余异常统一抛出SYSTEM_ERROR
	 */
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		
		try{

			OauthUserInfoResult oauthUserInfo = rpcUserService.getOauthUserInfoByUsernameResult(username).getResultDataThrowError();

			List<GrantedAuthority> grantedAuthorities = CollectionUtil.newArrayList();
			oauthUserInfo.getAuthorities().forEach((permission)->{
				grantedAuthorities.add(new SimpleGrantedAuthority(permission));
			});

			// 将用户ID写入附加属性，颁发令牌时会合并进claims
			Map<String, Object> attributes = CollectionUtil.newHashMap();
			attributes.put(OAuth2TokenUserClaimConstants.USER_ID,oauthUserInfo.getUserId());

			return new SecurityUserDetails(username,oauthUserInfo.getPassword(), grantedAuthorities, attributes);

		}catch (Exception e){

			// 业务错误（如用户不存在）转为用户认证失败，其余统一转为系统错误
			if(e instanceof BusinessResultException be && be.getErrorResult().isBusinessError()){
				throw new BusinessResultException(SystemErrorCode.USER_AUTH_ERROR);
			}

			throw new BusinessResultException(SystemErrorCode.SYSTEM_ERROR);

		}

	}

}
