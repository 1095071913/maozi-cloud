package com.maozi.service.config;

import com.maozi.service.api.annotation.RemoteResource;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.jetbrains.annotations.NotNull;
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
import java.util.concurrent.ConcurrentMap;

/**
 * {@link RemoteResource} 字段注入处理器（微服务模式）。
 * <p>
 * {@link RemoteResource} 是纯标记注解，无法借助 {@code @DubboReference} 元注解方式完成注入；
 * 本处理器扫描其标注的字段，通过 Dubbo 编程式 API（{@link ReferenceConfig} + {@link DubboBootstrap}）
 * 创建远程引用代理并反射写入（代理按接口类型缓存），同时按字段名将代理注册为 Spring 单例，
 * 供 {@code SpringUtil.getBean(字段名)} 等按名查找使用（如 {@code BaseServiceImpl} 的
 * {@code @QueryMapping(serviceName=...)} 关联数据解析机制）。
 * </p>
 * <p>
 * 属性填充阶段写入的是原始实例；若 Bean 之后被 AOP 包装为 CGLIB 代理（如 {@code @Transactional}），
 * 代理实例的字段不参与方法拦截、默认为 {@code null}，外部经代理直接读字段（如 {@code service.rpcXxx}）
 * 会得到 {@code null}。故本处理器在初始化完成后对 CGLIB 代理实例再同步写入一次字段值。
 * </p>
 *
 * @author pengjinlong
 * @since 2026/6/25
 */
@Component
public class RemoteResourceBeanPostProcessor implements InstantiationAwareBeanPostProcessor, BeanFactoryAware {

	/** 按接口类型缓存的远程引用代理 */
	private final ConcurrentMap<Class<?>, Object> referenceCache = new ConcurrentHashMap<>();

	/** 已注册的单例名称，避免重复注册 */
	private final Set<String> registeredNames = ConcurrentHashMap.newKeySet();

	/** Spring Bean 工厂，用于按字段名注册引用代理单例 */
	private ConfigurableListableBeanFactory beanFactory;

	/**
	 * 保存 Bean 工厂供单例注册使用
	 *
	 * @param beanFactory Spring Bean 工厂
	 */
	@Override
	public void setBeanFactory(@NotNull BeanFactory beanFactory) {
		this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
	}

	/**
	 * 在属性注入阶段为 {@link RemoteResource} 标注的字段写入 Dubbo 引用代理
	 * <p>
	 * 跳过静态字段；代理按接口类型缓存，并按字段名注册为 Spring 单例后反射写入目标字段。
	 * </p>
	 *
	 * @param pvs 原始属性值
	 * @param bean 当前正在创建的 bean 实例
	 * @param beanName bean 名称
	 * @return 原始属性值（不修改常规属性注入流程）
	 */
	@Override
	public PropertyValues postProcessProperties(@NotNull PropertyValues pvs, Object bean, @NotNull String beanName) throws BeansException {
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
	 * @param bean 初始化完成的 Bean 实例（可能是 CGLIB 代理）
	 * @param beanName Bean 名称
	 * @return 原始 Bean 实例（本处理器不替换对象）
	 * @throws BeansException 创建引用或注入时发生异常
	 */
	@Override
	public Object postProcessAfterInitialization(Object bean, @NotNull String beanName) throws BeansException {
		if (AopUtils.isCglibProxy(bean)) {
			injectRemoteResourceFields(bean, AopUtils.getTargetClass(bean));
		}
		return bean;
	}

	/**
	 * 将 {@link RemoteResource} 标注的非静态字段解析并反射写入指定实例。
	 * <p>
	 * 遍历指定类的所有字段（含父类继承的字段），通过 {@link #referenceCache} 按接口类型
	 * 获取（或创建）Dubbo 引用代理，按字段名注册为单例后写入实例字段；实例可以是原始 Bean，
	 * 也可以是其 CGLIB 代理（代理是子类实例，写入父类声明的字段合法）。
	 * </p>
	 *
	 * @param instance 待写入的实例（原始 Bean 或 CGLIB 代理）
	 * @param clazz 字段扫描基准类（代理场景传入 {@link AopUtils#getTargetClass} 的用户类）
	 */
	private void injectRemoteResourceFields(Object instance, Class<?> clazz) {
		ReflectionUtils.doWithFields(clazz, field -> {
			if (field.isAnnotationPresent(RemoteResource.class) && !Modifier.isStatic(field.getModifiers())) {
				Object reference = referenceCache.computeIfAbsent(field.getType(), this::createReference);
				registerSingletonIfAbsent(field.getName(), reference);
				ReflectionUtils.makeAccessible(field);
				ReflectionUtils.setField(field, instance, reference);
			}
		});
	}

	/**
	 * 按名称将引用代理注册为 Spring 单例（若尚未注册）。
	 * <p>
	 * 使 {@code SpringUtil.getBean(字段名)} 等按名查找可用，兼容项目中
	 * {@code BaseServiceImpl} 通过 {@code @QueryMapping(serviceName=...)} 解析关联数据的机制。
	 * </p>
	 *
	 * @param name     单例名称（取自字段名）
	 * @param singleton 引用代理实例
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

	/**
	 * 为指定接口创建 Dubbo 远程引用代理。
	 * <p>
	 * 调用 {@link ReferenceConfig#get(boolean)} 会自动触发 Dubbo 模块的初始化与启动，
	 * 无需关心启动时序；代理通过 {@link DubboBootstrap#reference(ReferenceConfig)} 注册，
	 * 由 Dubbo 统一管理生命周期。
	 * </p>
	 *
	 * @param interfaceClass 远程服务接口类型
	 * @return Dubbo 引用代理
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	private Object createReference(Class<?> interfaceClass) {
		ReferenceConfig reference = new ReferenceConfig();
		reference.setInterface(interfaceClass);
		DubboBootstrap.getInstance().reference(reference);
		return reference.get(true);
	}

}
