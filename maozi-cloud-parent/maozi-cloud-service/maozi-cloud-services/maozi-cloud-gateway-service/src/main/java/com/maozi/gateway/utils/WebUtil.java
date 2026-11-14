package com.maozi.gateway.utils;

import com.maozi.common.ObjectUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;

import java.util.List;
import java.util.Objects;

/**
 * Web 工具类
 * <p>
 * 提供从 HTTP 请求中获取客户端真实 IP 地址的能力，
 * 兼容 Nginx、Apache、WebLogic 等多种反向代理场景。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
public class WebUtil {

	/** 获取客户端真实 IP 的代理请求头，按优先级从高到低排列 */
	private static final List<String> CLIENT_IP_HEADERS = List.of(
			"x-forwarded-for", "Proxy-Client-IP", "WL-Proxy-Client-IP",
			"HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR", "X-Real-IP");

	/**
	 * 获取客户端真实 IP 地址
	 * <p>
	 * 按优先级依次从 X-Forwarded-For、Proxy-Client-IP、WL-Proxy-Client-IP、
	 * HTTP_CLIENT_IP、HTTP_X_FORWARDED_FOR、X-Real-IP 请求头中获取，
	 * 均无效时取请求远程地址兜底；
	 * X-Forwarded-For 含多个 IP（客户端IP, 代理1, 代理2...）时取第一个，
	 * IPv6 本地回环地址（0:0:0:0:0:0:0:1）转换为 IPv4 的 127.0.0.1。
	 * </p>
	 *
	 * @param request 服务端 HTTP 请求对象
	 * @return 客户端真实 IP 地址
	 */
	public static String getRequestHost(ServerHttpRequest request) {

		HttpHeaders headers = request.getHeaders();

		// 按优先级依次从反向代理 header 中获取
		String requestHost = null;
		for (String header : CLIENT_IP_HEADERS) {
			requestHost = headers.getFirst(header);
			if (!isBlankOrUnknown(requestHost)) {
				break;
			}
		}

		// 兜底：直接获取远程地址
		if (isBlankOrUnknown(requestHost)) {
			requestHost = Objects.requireNonNull(request.getRemoteAddress()).getAddress().getHostAddress();
		}

		// 处理多 IP 情况（x-forwarded-for 会返回 客户端IP,代理1,代理2...）
		if (requestHost != null && requestHost.contains(",")) {
			// 固定取第一段并去除首尾空白（x-forwarded-for 首位即客户端真实 IP）
			requestHost = requestHost.split(",")[0].trim();
		}

		// 处理 IPv6 本地回环地址，转换为 IPv4 格式
		if (com.maozi.common.WebUtil.LOCAL_LOOPBACK_IP.equals(requestHost)) {
			requestHost = com.maozi.common.WebUtil.LOCAL_IP;
		}

		return requestHost;

	}

	/**
	 * 统一判断：IP 是否为 null、空字符串、纯空白字符串或 unknown（忽略大小写）。
	 *
	 * @param ip 待检查的 IP 字符串
	 * @return 如果 IP 为空、空白或 "unknown" 则返回 true
	 */
	private static boolean isBlankOrUnknown(String ip) {
		return ObjectUtil.isNullEmpty(ip) || ip.isBlank() || com.maozi.common.WebUtil.UNKNOWN.equalsIgnoreCase(ip);
	}

}
