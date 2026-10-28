package com.maozi.mvc.config.rest;

import jakarta.annotation.Nonnull;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;

import java.io.IOException;

/**
 * RestTemplate 构建器
 * <p>
 * 通过 {@link org.springframework.boot.web.client.RestTemplateBuilder} 构建全局共用的
 * 自定义 {@link RestTemplate} 实例（扩展 Spring 的 RestTemplate，在 doExecute 中集成
 * 请求链路日志与耗时统计），并自定义错误处理器：当响应状态码为 400 时不抛出异常，
 * 交由调用方自行处理响应内容，其余错误状态码仍按 Spring 默认行为处理。
 * </p>
 *
 * @author maozi
 */
public class RestTemplateBuilder {

	/** 构建带链路日志增强与 400 容错错误处理器的 RestTemplate */
	public static RestTemplate buildRestTemplate(org.springframework.boot.web.client.RestTemplateBuilder restTemplateBuilder) {
		RestTemplate restTemplate = restTemplateBuilder.build(RestTemplate.class);
		restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {

			/**
			 * 重写错误处理逻辑，400 状态码不抛出异常
			 *
			 * @param response HTTP 客户端响应
			 * @throws IOException 读取响应体失败时抛出
			 */
			@Override
			public void handleError(@Nonnull ClientHttpResponse response) throws IOException {
				if (response.getStatusCode().value() != 400) {
					super.handleError(response);
				}
			}

		});
		return restTemplate;
	}

}