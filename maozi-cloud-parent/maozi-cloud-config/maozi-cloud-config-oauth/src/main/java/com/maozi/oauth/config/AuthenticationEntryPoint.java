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

package com.maozi.oauth.config;

import com.maozi.common.ObjectUtil;
import com.maozi.common.ResultUtil;
import com.maozi.common.WebUtil;
import com.maozi.common.result.AbstractBaseResult;
import com.maozi.common.result.error.code.SystemErrorCode;
import com.maozi.common.result.error.exception.BusinessResultException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;

/**
 * 认证入口点处理器
 * <p>
 * 当未认证用户尝试访问受保护资源时，返回认证错误的统一响应。
 * 优先从异常链中提取 {@link BusinessResultException} 携带的错误信息，
 * 否则返回默认的用户认证错误。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
public class AuthenticationEntryPoint implements org.springframework.security.web.AuthenticationEntryPoint {

    /**
     * 处理认证异常，返回认证错误响应
     *
     * @param request HTTP 请求
     * @param response HTTP 响应
     * @param authException 认证异常
     */
    @Override
    public void commence(HttpServletRequest request,HttpServletResponse response,AuthenticationException authException) {

        // 获取认证异常的直接原因（Spring Security会将自定义异常包装在AuthenticationException中）
        Throwable causeException = authException.getCause();

        // 尝试从异常链中提取BusinessResultException：
        // 如果异常链第二层（authException 的 cause 的 cause）是BusinessResultException，
        // 则使用其携带的错误信息（保留了原始的业务错误详情）；否则使用默认的用户认证错误码返回
        AbstractBaseResult<?> errorResult = ObjectUtil.isNotNullEmpty(causeException) && ObjectUtil.isNotNullEmpty(causeException.getCause()) && causeException.getCause() instanceof BusinessResultException businessResultException ?
                businessResultException.getErrorResult()
                :
                ResultUtil.error(SystemErrorCode.USER_AUTH_ERROR).autoIdentifyHttpCode(SystemErrorCode.USER_AUTH_ERROR_DEFAULT_CODE);

        // 将错误结果写入HTTP响应体
        WebUtil.writeResponseBody(response,errorResult);

    }

}
