package com.maozi.sentinel.config;

import com.alibaba.csp.sentinel.context.ContextUtil;
import com.alibaba.csp.sentinel.node.Node;
import com.maozi.common.monitor.Alarm;
import org.springframework.stereotype.Component;

/**
 * Sentinel 限流熔断告警实现
 * <p>
 * 基于 Sentinel 的上下文节点统计阻塞 QPS，表示请求被限流或熔断。
 * 通过 {@code @Component} 注册为 Spring Bean，供日志切面通过依赖注入调用，
 * 实现日志模块与 Sentinel 底层 API 的解耦。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
@Component
public class SentinelAlarm implements Alarm {

	/**
	 * 统计 Sentinel 阻塞 QPS（表示请求被限流或熔断）
	 * <p>
	 * 从 Sentinel 上下文获取当前节点，将其阻塞 QPS 计数加 1。
	 * </p>
	 */
	@Override
	public void alarm() {
		Node curNode = ContextUtil.getContext().getCurNode();
		curNode.increaseBlockQps(1);
	}

}
