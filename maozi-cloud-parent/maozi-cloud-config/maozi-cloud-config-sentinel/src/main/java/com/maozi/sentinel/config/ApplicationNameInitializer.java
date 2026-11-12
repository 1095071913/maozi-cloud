package com.maozi.sentinel.config;

import com.alibaba.csp.sentinel.config.SentinelConfig;
import com.maozi.common.ObjectUtil;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Sentinel 应用名称初始化器
 * <p>
 * Spring Cloud Alibaba 的 {@code SentinelApplicationContextInitializer} 启动时读取
 * {@code spring.application.name} 写入 Sentinel 应用名称系统属性
 * {@code csp.sentinel.app.name}（仅在该属性为空时写入），该名称用于
 * Sentinel 控制台注册、心跳上报与监控数据归属。当 {@code spring.application.name}
 * 不能调整但 Sentinel 需以其他名称注册时，本初始化器提供覆盖能力：
 * 配置 {@code spring.cloud.sentinel.name} 后，以该值优先写入
 * {@code csp.sentinel.app.name}，使 Spring Cloud Alibaba 的回退写入被跳过；
 * 未配置时行为不变，仍回退 {@code spring.application.name}。
 * </p>
 * <p>
 * 本初始化器通过 spring.factories 注册并返回最高优先级，保证先于
 * Spring Cloud Alibaba 的同名扩展点执行：一是先占位使其跳过回退写入，
 * 二是先于 {@code spring.cloud.sentinel.eager=true} 触发的 Sentinel 提前初始化生效
 * （Sentinel 应用名称在核心配置类 {@link SentinelConfig} 类加载时一次性解析，
 * 之后不再读取该系统属性）。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
public class ApplicationNameInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext>, Ordered {

	/** Sentinel 应用名称覆盖配置项，配置后 Sentinel 以该值注册，而非 spring.application.name */
	private static final String APPLICATION_NAME_KEY = "spring.cloud.sentinel.name";

	/**
	 * 返回最高优先级
	 * <p>
	 * 保证先于 Spring Cloud Alibaba 的 Sentinel 上下文初始化器执行。
	 * </p>
	 *
	 * @return 排序优先级
	 */
	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}

	/**
	 * 以 spring.cloud.sentinel.name 覆盖 Sentinel 应用名称
	 * <p>
	 * 环境此时已完成配置装载（含 Nacos 导入文件），读取
	 * {@code spring.cloud.sentinel.name} 并在非空时写入系统属性
	 * {@code csp.sentinel.app.name}，占位后 Spring Cloud Alibaba
	 * 不再用 {@code spring.application.name} 覆写。
	 * </p>
	 *
	 * @param applicationContext 应用上下文
	 */
	@Override
	public void initialize(ConfigurableApplicationContext applicationContext) {

		ConfigurableEnvironment environment = applicationContext.getEnvironment();
		String applicationName = environment.getProperty(APPLICATION_NAME_KEY);

		if (ObjectUtil.isNotNullEmpty(applicationName)) {
			System.setProperty(SentinelConfig.APP_NAME_PROP_KEY, applicationName);
		}

	}

}
