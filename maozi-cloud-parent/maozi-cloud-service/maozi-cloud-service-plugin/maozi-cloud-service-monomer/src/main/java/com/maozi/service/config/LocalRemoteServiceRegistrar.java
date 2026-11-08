package com.maozi.service.config;

import com.maozi.service.api.annotation.RemoteService;
import jakarta.annotation.Nonnull;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;

/**
 * 单体服务 {@code @RemoteService} 本地 Bean 注册器。
 * <p>
 * 单体服务（maozi-cloud-all-service）不引入 maozi-cloud-service-distributed（Dubbo 模块），
 * {@link RemoteService} 标注的 RPC 实现类不会被任何处理器注册为 Spring Bean，
 * 导致 {@code @RemoteResource} 字段即使有处理器也找不到可注入的实例。
 * </p>
 * <p>
 * 本注册器在 Bean 定义阶段扫描 {@link RemoteService} 类并注册为本地 Bean，
 * 使 {@code @RemoteResource} 字段可按接口类型从本地容器获取实例。
 * 注册时设 {@code autowireCandidate = false}，与分布式模块的
 * {@code RemoteServiceBeanPostProcessor} 行为保持一致：
 * 避免 RPC 实现类与其父类 {@code @Service} Bean 在共享接口上产生装配歧义
 * （如 {@code RpcUserServiceImpl extends UserServiceImpl}，二者都实现 {@code UserService}）。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
@Component
public class LocalRemoteServiceRegistrar implements BeanDefinitionRegistryPostProcessor {

	/** 扫描根包，覆盖所有业务模块 */
	private static final String BASE_PACKAGE = "com.maozi";

	/**
	 * 扫描 {@link RemoteService} 类并注册为本地 Bean 定义。
	 * <p>
	 * 使用 {@link ClassPathScanningCandidateComponentProvider} 按注解过滤，
	 * 关闭默认的 {@code @Component} 过滤器以精确匹配 {@link RemoteService}。
	 * 每个候选设 {@code autowireCandidate = false} 后注册，避免共享接口歧义。
	 * </p>
	 *
	 * @param registry Bean 定义注册表
	 */
	@Override
	public void postProcessBeanDefinitionRegistry(@Nonnull BeanDefinitionRegistry registry) {

		ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
		scanner.addIncludeFilter(new AnnotationTypeFilter(RemoteService.class));

		AnnotationBeanNameGenerator nameGenerator = new AnnotationBeanNameGenerator();

		for (BeanDefinition candidate : scanner.findCandidateComponents(BASE_PACKAGE)) {
			String beanName = nameGenerator.generateBeanName(candidate, registry);
			candidate.setAutowireCandidate(false);
			registry.registerBeanDefinition(beanName, candidate);
		}

	}

	/**
	 * Bean 工厂后处理，本注册器无需额外处理。
	 *
	 * @param beanFactory 可配置的 Bean 工厂
	 */
	@Override
	public void postProcessBeanFactory(@Nonnull ConfigurableListableBeanFactory beanFactory) {
	}

}
