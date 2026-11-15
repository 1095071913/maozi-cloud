package com.maozi.oauth.token.config.handle;

import com.maozi.common.ObjectUtil;
import com.maozi.common.ResultUtil;
import com.maozi.common.WebUtil;
import com.maozi.common.result.error.ErrorResult;
import com.maozi.common.result.error.code.SystemErrorCode;
import com.maozi.common.result.error.exception.BusinessResultException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;

/**
 * 用户认证失败处理器
 * <p>
 * 当用户认证失败时（如用户名或密码错误），
 * 返回用户认证错误（USER_AUTH_ERROR）的统一错误响应。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Slf4j
public class UserAuthenticationFailureHandler implements
        org.springframework.security.web.authentication.AuthenticationFailureHandler {

    /**
     * 处理用户认证失败
     * <p>
     * 用户认证失败后，向响应中写入用户认证错误的统一错误信息。
     * </p>
     *
     * @param request   HTTP请求对象
     * @param response  HTTP响应对象
     * @param authException 认证异常信息
     */
    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
            HttpServletResponse response, AuthenticationException authException) {

        // 获取认证异常的直接原因
        // （Spring Security会将自定义异常包装在AuthenticationException中）
        Throwable causeException = authException.getCause();

        // 尝试从异常链中提取BusinessResultException：
        // 如果异常链第二层（authException 的 cause 的 cause）是BusinessResultException，
        // 则使用其携带的错误信息（保留了原始的业务错误详情）；
        // 否则使用默认的用户认证错误码返回

        ErrorResult<?> errorResult = null;
        if(ObjectUtil.isNotNullEmpty(causeException)
            && ObjectUtil.isNotNullEmpty(causeException.getCause())
            && causeException.getCause() instanceof BusinessResultException businessResultException){
            
            errorResult = businessResultException.getErrorResult().autoIdentifyHttpCode();
            if(errorResult.isSystemError()){
                log.error("用户认证授权内部服务错误", businessResultException);
            }
            
        }else{
            errorResult = ResultUtil.error(SystemErrorCode.USER_AUTH_ERROR)
                .autoIdentifyHttpCode(SystemErrorCode.USER_AUTH_ERROR_DEFAULT_CODE);
        }

        // 将错误结果写入HTTP响应体
        WebUtil.writeResponseBody(response,errorResult);
        
    }

}
