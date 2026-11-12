package com.maozi.config;

import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 跨域请求处理过滤器
 * <p>
 * 聚合服务（maozi-cloud-all-service）作为对外入口，直接接收浏览器前端请求，
 * 需要处理跨域资源共享（CORS），与网关服务（{@code GatewayCrossConfig}）的
 * 响应式跨域过滤器语义保持一致。
 * </p>
 * <p>
 * 本过滤器（{@code SecurityProperties.DEFAULT_FILTER_ORDER - 2}）优先级高于
 * Spring Security 过滤器链与 {@link StripUserInfoHeaderFilter}
 * （{@code SecurityProperties.DEFAULT_FILTER_ORDER - 1}）：
 * OPTIONS 预检请求不携带访问令牌，若进入认证过滤器链会被直接拒绝（401），
 * 因此预检请求在本过滤器内补充跨域响应头后直接返回 200，不再向下转发。
 * </p>
 * <p>
 * 由于允许携带凭证（{@code Access-Control-Allow-Credentials: true}）时
 * {@code Access-Control-Allow-Origin} 不允许取值为 {@code *}，
 * 故采用回显请求 {@code Origin} 的方式放行来源域。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Component
@Order(SecurityProperties.DEFAULT_FILTER_ORDER - 2)
public class CrossOriginFilter extends OncePerRequestFilter {

	/** 暴露给前端的响应头取值：所有响应头 */
	private static final String ALL = "*";

	/** CORS 预检请求缓存时间（秒） */
	private static final String MAX_AGE = "3600";

	/**
	 * 为跨域请求补充 CORS 响应头
	 * <p>
	 * 非跨域请求直接放行；跨域请求统一设置来源域、凭证与暴露响应头，
	 * 其中预检请求（OPTIONS）额外设置允许的请求头、HTTP 方法与缓存时间后
	 * 直接返回 200，不进入后续过滤器链。
	 * </p>
	 *
	 * @param request     原始 HTTP 请求
	 * @param response    HTTP 响应
	 * @param filterChain 过滤器链
	 * @throws ServletException Servlet 异常
	 * @throws IOException      IO 异常
	 */
	@Override
	protected void doFilterInternal(@Nonnull HttpServletRequest request, @Nonnull HttpServletResponse response, @Nonnull FilterChain filterChain) throws ServletException, IOException {

		// 非跨域请求直接放行
		if (!CorsUtils.isCorsRequest(request)) {
			filterChain.doFilter(request, response);
			return;
		}

		// 允许请求来源域（回显 Origin，配合允许凭证时不可使用 *）
		response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, request.getHeader(HttpHeaders.ORIGIN));
		// 允许携带凭证（Cookies）
		response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
		// 暴露给前端的响应头
		response.setHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, ALL);

		// 预检请求：补充预检专用响应头后直接返回 200，避免被认证过滤器链拒绝
		if (CorsUtils.isPreFlightRequest(request)) {
			// 允许的请求头：回显预检请求声明的头
			String allowHeaders = request.getHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
			if (allowHeaders != null) {
				response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, allowHeaders);
			}
			// 允许的 HTTP 方法：回显预检请求声明的方法
			String allowMethods = request.getHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD);
			if (allowMethods != null) {
				response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, allowMethods);
			}
			// 预检请求缓存时间
			response.setHeader(HttpHeaders.ACCESS_CONTROL_MAX_AGE, MAX_AGE);

			response.setStatus(HttpServletResponse.SC_OK);
			return;
		}

		filterChain.doFilter(request, response);
	}

}
