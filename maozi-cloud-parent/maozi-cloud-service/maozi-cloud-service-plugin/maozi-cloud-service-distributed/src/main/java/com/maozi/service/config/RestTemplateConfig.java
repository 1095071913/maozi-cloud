package com.maozi.service.config;

import com.alibaba.cloud.sentinel.annotation.SentinelRestTemplate;
import com.alibaba.cloud.sentinel.rest.SentinelClientHttpResponse;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.authority.AuthorityException;
import com.maozi.common.JacksonUtil;
import com.maozi.common.ResultUtil;
import com.maozi.common.result.error.code.ErrorCode;
import com.maozi.common.result.error.code.SystemErrorCode;
import com.maozi.mvc.config.rest.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestTemplate;

/**
 * 分布式 RestTemplate HTTP 客户端配置
 * <p>
 * 通过 {@link org.springframework.boot.web.client.RestTemplateBuilder} 构建全局共用的
 * 自定义 {@link RestTemplate} 实例（链路日志增强，且响应状态码为 400 时不抛出异常，
 * 交由调用方自行处理响应内容，其余错误状态码仍按 Spring 默认行为处理），
 * 并通过 {@code @SentinelRestTemplate} 接入 Sentinel 流量防护：
 * 请求被限流或黑名单拦截时，由 {@link #handleException} 兜底返回对应错误码的 JSON 响应。
 * </p>
 *
 * @author maozi
 */
@Configuration
public class RestTemplateConfig {

	/** 构建接入 Sentinel 流量防护的全局 RestTemplate */
	@Bean
	@SentinelRestTemplate(blockHandler = "handleException", blockHandlerClass = RestTemplateConfig.class)
	public RestTemplate restTemplate(org.springframework.boot.web.client.RestTemplateBuilder restTemplateBuilder) {
		return RestTemplateBuilder.buildRestTemplate(restTemplateBuilder);
	}

	/**
	 * Sentinel 阻断（限流/黑名单）时的统一兜底响应
	 * <p>
	 * 授权规则拦截（黑名单）返回 {@link SystemErrorCode#BLACKLIST_ERROR}，
	 * 其余阻断（如限流）返回 {@link SystemErrorCode#CURRENT_LIMITING_ERROR}，
	 * 响应体为 JSON 序列化的统一错误结果。
	 * </p>
	 */
	public static ClientHttpResponse handleException(HttpRequest request, byte[] body, ClientHttpRequestExecution execution, BlockException exception) {
		ErrorCode errorCode = exception instanceof AuthorityException ?
				SystemErrorCode.BLACKLIST_ERROR
				:
				SystemErrorCode.CURRENT_LIMITING_ERROR;
		return new SentinelClientHttpResponse(JacksonUtil.objectToJson(ResultUtil.error(errorCode)));
	}

}