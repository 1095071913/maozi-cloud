package com.maozi.oauth.token.config.handle;

import com.maozi.common.ResultUtil;
import com.maozi.common.WebUtil;
import com.maozi.common.result.error.code.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

/**
 * 客户端认证失败处理器
 * <p>
 * 当客户端认证失败时（如客户端ID或密钥错误），返回401状态码和
 * "客户端认证授权失败"的错误信息。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
public class ClientAuthenticationFailureHandler implements AuthenticationFailureHandler {

    /** 未认证的HTTP状态码 */
    private static final int NO_AUTHENTICATION_CODE = 401;

    /** 客户端认证失败的错误码和错误信息 */
    private static final ErrorCode NO_AUTHENTICATION_CODE_DATA = new ErrorCode(NO_AUTHENTICATION_CODE, "客户端认证授权失败");

    /**
     * 处理客户端认证失败
     * <p>
     * 当客户端认证失败时，向响应中写入401错误和对应的错误信息。
     * </p>
     *
     * @param request   HTTP请求对象
     * @param response  HTTP响应对象
     * @param exception 认证异常信息
     */
    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) {
        WebUtil.writeResponseBody(response,ResultUtil.error(NO_AUTHENTICATION_CODE_DATA).autoIdentifyHttpCode(NO_AUTHENTICATION_CODE));
    }

}