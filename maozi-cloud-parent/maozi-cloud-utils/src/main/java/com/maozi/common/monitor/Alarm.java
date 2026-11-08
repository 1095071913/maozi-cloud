package com.maozi.common.monitor;

/**
 * 限流熔断告警接口
 * <p>
 * 定义请求被限流、熔断或处理失败时的告警行为。
 * 由具体的监控组件（如 Sentinel）提供实现，并在 Spring 容器中注册为 Bean，
 * 供日志切面等组件通过依赖注入调用，从而对底层监控实现解耦。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
public interface Alarm {

	/**
	 * 触发告警
	 * <p>
	 * 调用时机：请求被限流、熔断或处理失败（业务错误、系统错误）时触发。
	 * 由实现方完成阻塞 QPS 统计或告警通知等具体动作。
	 * </p>
	 */
	void alarm();

}
