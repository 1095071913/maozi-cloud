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

package com.maozi.oauth.token.config.handle;

import com.maozi.common.ResultUtil;
import com.maozi.common.WebUtil;
import com.maozi.common.result.AbstractBaseResult;
import com.maozi.common.result.error.code.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/**
 * 客户端认证入口点
 * <p>
 * 当客户端未进行认证授权时，返回401状态码和"客户端未认证授权"的错误信息。
 * 用于处理需要客户端认证但客户端未提供有效凭证的请求。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
public class ClientAuthenticationEntryPoint implements AuthenticationEntryPoint {

	/** 未认证的HTTP状态码 */
	private static final int NO_AUTHENTICATION_CODE = 401;

	/** 未认证的错误码和错误信息 */
	private static final ErrorCode NO_AUTHENTICATION_CODE_DATA = new ErrorCode(NO_AUTHENTICATION_CODE,"客户端未认证授权");

	/**
	 * 处理未认证的客户端请求
	 * <p>
	 * 当客户端未提供有效的认证信息时，向响应中写入401错误和对应的错误信息。
	 * </p>
	 *
	 * @param request       HTTP请求对象
	 * @param response      HTTP响应对象
	 * @param authException 认证异常信息
	 */
	@Override
	public void commence(HttpServletRequest request,HttpServletResponse response,AuthenticationException authException) {

		AbstractBaseResult<Object> error = ResultUtil.error(NO_AUTHENTICATION_CODE_DATA).autoIdentifyHttpCode(NO_AUTHENTICATION_CODE);
			
		WebUtil.writeResponseBody(response,error);
		
	}
	
}