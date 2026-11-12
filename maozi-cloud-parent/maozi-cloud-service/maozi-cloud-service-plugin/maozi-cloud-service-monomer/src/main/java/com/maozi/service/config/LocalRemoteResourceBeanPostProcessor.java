package com.maozi.service.config;

import com.maozi.service.api.annotation.RemoteResource;
import jakarta.annotation.Nonnull;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 单体服务 {@code @RemoteResource} 字段本地注入处理器。
 * <p>
 * 单体服务（maozi-cloud-all-service）不引入 Dubbo 模块，注解上的 {@code @Resource} 元注解也不被
 * Spring 识别，故由本处理器在 Bean 属性填充阶段按字段声明的接口类型从本地容器获取 Bean 实例并反射写入，
 * 同时按字段名注册为 Spring 单例，供 {@code SpringUtil.getBean(字段名)} 等按名查找使用
 * （{@code BaseServiceImpl} 的 {@code @QueryMapping(serviceName=...)} 关联数据解析依赖于此）。
 * {@code getBean(Class)} 在仅有一个候选时忽略 {@code autowireCandidate} 标志，
 * 因此 {@link LocalRemoteServiceRegistrar} 注册的 {@code autowireCandidate = false} 的
 * RPC 实现类仍可被正确解析。
 * </p>
 * <p>
 * 属性填充阶段写入的是原始实例；若 Bean 之后被 AOP 包装为 CGLIB 代理（如 {@code @Transactional}），
 * 代理实例的字段不参与方法拦截、默认为 {@code null}，外部经代理直接读字段（如 {@code service.rpcXxx}）
 * 会得到 {@code null}。故本处理器在初始化完成后对 CGLIB 代理实例再同步写入一次字段值。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Component
public class LocalRemoteResourceBeanPostProcessor implements InstantiationAwareBeanPostProcessor, BeanFactoryAware {

	/** Spring Bean 工厂，用于按类型查找本地 Bean */
	private ConfigurableListableBeanFactory beanFactory;

	/** 已注册的单例名称，避免重复注册 */
	private final Set<String> registeredNames = ConcurrentHashMap.newKeySet();

	/**
	 * 注入 Bean 工厂。
	 *
	 * @param beanFactory Spring Bean 工厂
	 */
	@Override
	public void setBeanFactory(@Nonnull BeanFactory beanFactory) {
		this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
	}

	/**
	 * 扫描 {@link RemoteResource} 字段并按接口类型从本地容器注入。
	 * <p>
	 * 遍历目标 Bean 的所有字段（含父类继承的字段），找到 {@link RemoteResource} 标注的非静态字段，
	 * 通过 {@code beanFactory.getBean(字段类型)} 从本地容器获取实例，按字段名注册为单例后反射写入。
	 * </p>
	 *
	 * @param pvs      属性值
	 * @param bean     正在创建的 Bean 实例
	 * @param beanName Bean 名称
	 * @return 原始属性值（本处理器不修改属性值，仅通过反射写字段）
	 * @throws BeansException 获取或注入 Bean 时发生异常
	 */
	@Override
	public PropertyValues postProcessProperties(@Nonnull PropertyValues pvs, @Nonnull Object bean, @Nonnull String beanName) throws BeansException {
		injectRemoteResourceFields(bean, bean.getClass());
		return pvs;
	}

	/**
	 * 对被 AOP 包装为 CGLIB 代理的 Bean 同步注入 {@link RemoteResource} 字段。
	 * <p>
	 * 属性填充阶段写入的是原始实例，而 CGLIB 代理是另行创建的子类实例（不走构造器、字段为默认值），
	 * 方法调用经代理委托不受影响，但外部直接读代理实例的字段会得到 {@code null}。
	 * 本处理器未实现 {@code Ordered}，在处理器链中位于事务/AOP 自动代理创建器之后，
	 * 因此此阶段拿到的 Bean 已是最终代理对象，可安全回填字段。
	 * </p>
	 *
	 * @param bean     初始化完成的 Bean 实例（可能是 CGLIB 代理）
	 * @param beanName Bean 名称
	 * @return 原始 Bean 实例（本处理器不替换对象）
	 * @throws BeansException 获取或注入 Bean 时发生异常
	 */
	@Override
	public Object postProcessAfterInitialization(@Nonnull Object bean, @Nonnull String beanName) throws BeansException {
		if (AopUtils.isCglibProxy(bean)) {
			injectRemoteResourceFields(bean, AopUtils.getTargetClass(bean));
		}
		return bean;
	}

	/**
	 * 将 {@link RemoteResource} 标注的非静态字段解析并反射写入指定实例。
	 * <p>
	 * 遍历指定类的所有字段（含父类继承的字段），通过 {@code beanFactory.getBean(字段类型)}
	 * 从本地容器获取实例，按字段名注册为单例后写入实例字段；实例可以是原始 Bean，也可以是
	 * 其 CGLIB 代理（代理是子类实例，写入父类声明的字段合法）。
	 * </p>
	 *
	 * @param instance 待写入的实例（原始 Bean 或 CGLIB 代理）
	 * @param clazz    字段扫描基准类（代理场景传入 {@link AopUtils#getTargetClass} 的用户类）
	 */
	private void injectRemoteResourceFields(Object instance, Class<?> clazz) {

		ReflectionUtils.doWithFields(clazz, field -> {
			if (field.isAnnotationPresent(RemoteResource.class) && !Modifier.isStatic(field.getModifiers())) {
				Object dependency = beanFactory.getBean(field.getType());
				registerSingletonIfAbsent(field.getName(), dependency);
				ReflectionUtils.makeAccessible(field);
				ReflectionUtils.setField(field, instance, dependency);
			}
		});

	}

	/**
	 * 按名称将依赖实例注册为 Spring 单例（若尚未注册）。
	 * <p>
	 * 使 {@code SpringUtil.getBean(字段名)} 等按名查找可用，兼容项目中
	 * {@code BaseServiceImpl} 通过 {@code @QueryMapping(serviceName=...)} 解析关联数据的机制。
	 * </p>
	 *
	 * @param name     单例名称（取自字段名）
	 * @param singleton 依赖实例
	 */
	private void registerSingletonIfAbsent(String name, Object singleton) {
		if (beanFactory != null && registeredNames.add(name)) {
			try {
				beanFactory.registerSingleton(name, singleton);
			} catch (IllegalStateException e) {
				// 容器中已存在同名 bean，回退标记并跳过
				registeredNames.remove(name);
			}
		}
	}

}
