package com.maozi.oauth.token.config.handle;

import com.maozi.common.ResultUtil;
import com.maozi.common.WebUtil;
import com.maozi.common.result.error.code.ErrorCode;
import com.maozi.common.result.error.code.SystemErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;

/**
 * 用户认证失败处理器
 * <p>
 * 当用户认证失败时（如用户名或密码错误），返回用户认证错误（USER_AUTH_ERROR）的统一错误响应。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
public class UserAuthenticationFailureHandler implements org.springframework.security.web.authentication.AuthenticationFailureHandler {

    /**
     * 处理用户认证失败
     * <p>
     * 用户认证失败后，向响应中写入用户认证错误的统一错误信息。
     * </p>
     *
     * @param request   HTTP请求对象
     * @param response  HTTP响应对象
     * @param exception 认证异常信息
     */
    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) {
        ErrorCode errorCode = SystemErrorCode.USER_AUTH_ERROR;
        WebUtil.writeResponseBody(response, ResultUtil.error(errorCode).autoIdentifyHttpCode(errorCode.getCode()));
    }

}