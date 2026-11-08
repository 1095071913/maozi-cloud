package com.maozi.oauth.token.config.handle;

import com.maozi.common.ResultUtil;
import com.maozi.common.WebUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;

/**
 * 令牌撤销成功处理器
 * <p>
 * 当令牌成功撤销后（即用户退出登录），向客户端返回成功响应。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
public class TokenRevocationSuccessHandler implements AuthenticationSuccessHandler {

    /**
     * 处理令牌撤销成功事件
     * <p>
     * 令牌撤销成功后，向响应中写入统一格式的成功信息。
     * </p>
     *
     * @param request       HTTP请求对象
     * @param response      HTTP响应对象
     * @param authentication 认证信息
     * @throws IOException      IO异常
     * @throws ServletException Servlet异常
     */
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        WebUtil.writeResponseBody(response,ResultUtil.success());
    }

}