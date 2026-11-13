package com.maozi.gateway.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Hooks;
import reactor.netty.channel.AbortedException;

/**
 * Reactor 全局丢弃异常钩子配置
 * <p>
 * 客户端在网关响应发送前断开连接时，Reactor Netty 抛出的 {@link AbortedException}
 * 因响应链路已取消而无订阅者接收，最终进入默认 onErrorDropped 钩子并以 ERROR
 * 级别输出日志；该异常发生在 {@code ErrorWebExceptionHandler} 之后，
 * GatewayExceptionHandler 无法拦截。本配置通过 {@link Hooks#onErrorDropped}
 * 注册自定义钩子：客户端断连类异常视为正常现象静默丢弃，其他丢弃异常保持
 * ERROR 级别记录，避免遗漏真实错误
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Slf4j
@Configuration
public class ReactorHooksConfiguration {

	/**
	 * 注册全局丢弃异常钩子
	 * <p>
	 * 在容器刷新阶段执行（早于 Netty 服务启动接收请求），
	 * 确保运行期所有丢弃异常均经过自定义钩子处理。
	 * </p>
	 */
	@PostConstruct
	public void registerOnErrorDroppedHook() {

		Hooks.onErrorDropped(e -> {

			// 客户端主动断开连接导致的 AbortedException 属于正常业务现象，静默丢弃，不记录日志
			if (e instanceof AbortedException) {
				return;
			}

			// 其他丢弃异常保持 ERROR 级别记录，避免遗漏真实错误
			log.error("Operator called custom onErrorDropped", e);

		});

	}

}
