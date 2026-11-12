package com.maozi.oauth.config;

import com.maozi.common.ResultUtil;
import com.maozi.common.WebUtil;
import com.maozi.common.result.error.code.ErrorCode;
import com.maozi.common.result.error.code.SystemErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.firewall.RequestRejectedException;

/**
 * 请求拒绝处理器
 * <p>
 * 当 Spring Security 防火墙检测到恶意或不符合规范的请求时，
 * 返回恶意请求的统一错误响应。使用 {@link SystemErrorCode#MALICE_REQUEST_ERROR} 作为错误码。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Configuration
public class RequestRejectedHandler implements org.springframework.security.web.firewall.RequestRejectedHandler {

    /**
     * 处理被拒绝的请求，返回恶意请求错误响应
     *
     * @param request HTTP 请求
     * @param response HTTP 响应
     * @param ex 请求拒绝异常
     */
   @Override
   public void handle(HttpServletRequest request, HttpServletResponse response,RequestRejectedException ex){
       // 获取恶意请求错误码
       ErrorCode errorCode = SystemErrorCode.MALICE_REQUEST_ERROR;
       // 构建错误响应并自动根据错误码设置HTTP状态码，写入响应体
       WebUtil.writeResponseBody(response, ResultUtil.error(errorCode).autoIdentifyHttpCode(errorCode.getCode()));
   }

}
