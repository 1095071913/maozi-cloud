package com.maozi.sentinel.config;

import cn.hutool.extra.servlet.JakartaServletUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * 请求来源解析器
 * <p>
 * 实现 Sentinel 的 {@link com.alibaba.csp.sentinel.adapter.spring.webmvc_v6x.callback.RequestOriginParser} 接口，
 * 从 HTTP 请求中解析来源标识（客户端真实 IP 地址），用于授权规则的来源判断。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Component
public class RequestOriginParser implements com.alibaba.csp.sentinel.adapter.spring.webmvc_v6x.callback.RequestOriginParser {

	/**
	 * 解析请求来源
	 *
	 * @param request HTTP 请求
	 * @return 请求来源标识（客户端真实 IP 地址，优先取反向代理头中的地址）
	 */
	@Override
	public String parseOrigin(HttpServletRequest request) {
		return JakartaServletUtil.getClientIP(request);
	}

}
