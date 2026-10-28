package com.maozi.sentinel.config;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.authority.AuthorityException;
import com.maozi.common.ResultUtil;
import com.maozi.common.WebUtil;
import com.maozi.common.result.error.code.ErrorCode;
import com.maozi.common.result.error.code.SystemErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

/**
 * Sentinel 流控拦截处理器
 * <p>
 * 实现 Sentinel 的 {@link com.alibaba.csp.sentinel.adapter.spring.webmvc_v6x.callback.BlockExceptionHandler} 接口，
 * 当请求被 Sentinel 规则拦截时返回统一错误响应：
 * 授权规则（黑名单）拦截使用 {@link SystemErrorCode#BLACKLIST_ERROR} 作为错误码，
 * 其余流控/熔断拦截使用 {@link SystemErrorCode#CURRENT_LIMITING_ERROR} 作为错误码。
 * </p>
 *
 * @author maozi
 */
@Component
public class BlockExceptionHandler implements com.alibaba.csp.sentinel.adapter.spring.webmvc_v6x.callback.BlockExceptionHandler {

	/**
	 * 处理被 Sentinel 拦截的请求，按拦截类型返回黑名单或限流错误响应
	 *
	 * @param request HTTP 请求
	 * @param response HTTP 响应
	 * @param resourceName 被拦截的资源名称
	 * @param exception Sentinel 阻塞异常
	 */
	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response, String resourceName, BlockException exception) {
		ErrorCode errorCode = exception instanceof AuthorityException ?
				SystemErrorCode.BLACKLIST_ERROR
				:
				SystemErrorCode.CURRENT_LIMITING_ERROR;
		WebUtil.writeResponseBody(response, ResultUtil.error(errorCode).autoIdentifyHttpCode(errorCode.getCode()));
	}

	

}
