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

import com.maozi.common.ResultUtil;
import com.maozi.common.WebUtil;
import com.maozi.common.result.error.code.ErrorCode;
import com.maozi.common.result.error.code.SystemErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

/**
 * 访问拒绝处理器
 * <p>
 * 当已认证用户尝试访问其没有权限的资源时，返回权限不足的统一错误响应。
 * 使用 {@link SystemErrorCode#PERMISSION_ERROR} 作为错误码。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
public class AccessDeniedHandler implements org.springframework.security.web.access.AccessDeniedHandler {

    /**
     * 处理访问拒绝异常，返回权限错误响应
     *
     * @param request HTTP 请求
     * @param response HTTP 响应
     * @param accessDeniedException 访问拒绝异常
     */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,AccessDeniedException accessDeniedException){
        // 获取权限不足的错误码
        ErrorCode errorCode = SystemErrorCode.PERMISSION_ERROR;
        // 构建错误响应并自动根据错误码设置HTTP状态码，写入响应体
        WebUtil.writeResponseBody(response, ResultUtil.error(errorCode).autoIdentifyHttpCode(errorCode.getCode()));
    }

}
