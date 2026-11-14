package com.maozi.mvc.config.rest;

import jakarta.annotation.Nonnull;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 接口声明路由的请求映射处理器
 * <p>
 * Spring MVC 默认仅沿方法声明类的类型层级解析路由注解，实现类直接继承基类而未重写的公共方法
 * 无法关联 REST 接口上声明的路由，导致映射注册失败（404）。本处理器在常规解析未命中时，
 * 于处理器类实现的接口中查找同签名且声明了映射注解的方法完成路由注册，并换用该接口方法构建
 * {@link HandlerMethod}，使参数注解与方法级安全注解正常生效；反射调用接口方法仍虚分派到
 * 基类实现，业务逻辑不变。
 * </p>
 *
 * @author maozi
 * @date 2026/11/13
 */
public class InterfaceMethodRequestMappingHandlerMapping extends RequestMappingHandlerMapping {

	/** 继承自父类的方法到接口声明方法的映射缓存，供注册阶段构建 HandlerMethod 时换用 */
	private final Map<Method, Method> interfaceMethodCache = new ConcurrentHashMap<>();

	/**
	 * 获取方法对应的请求映射信息
	 * <p>
	 * 先执行常规解析（覆盖现有实现类自行声明或重写方法的场景）；
	 * 未命中时在处理器类实现的接口中查找同签名且声明了映射注解的方法，
	 * 命中则基于接口方法生成映射信息并登记缓存。
	 * </p>
	 *
	 * @param method 处理器方法
	 * @param handlerType 处理器类型
	 * @return 请求映射信息；常规解析与接口查找均未命中时返回 {@code null}
	 */
	@Override
	protected RequestMappingInfo getMappingForMethod(@Nonnull Method method, @Nonnull Class<?> handlerType) {

		RequestMappingInfo info = super.getMappingForMethod(method, handlerType);
		if (info == null) {

			Method interfaceMethod = findMappedInterfaceMethod(method, handlerType);
			if (interfaceMethod != null) {

				info = super.getMappingForMethod(interfaceMethod, handlerType);
				if (info != null) {
					interfaceMethodCache.put(method, interfaceMethod);
				}

			}

		}

		return info;

	}

	/**
	 * 创建处理器方法实例
	 * <p>
	 * 若该方法为接口声明路由的继承实现，则换用接口方法构建 {@link HandlerMethod}，
	 * 使参数注解与方法级安全注解可直接从接口方法上解析。
	 * </p>
	 *
	 * @param handler 处理器（Bean 名称或实例）
	 * @param method 注册的处理方法
	 * @return 处理器方法实例
	 */
	@Override
	protected HandlerMethod createHandlerMethod(@Nonnull Object handler, @Nonnull Method method) {
		Method interfaceMethod = interfaceMethodCache.get(method);
		return super.createHandlerMethod(handler, interfaceMethod != null ? interfaceMethod : method);
	}

	/**
	 * 在处理器类实现（含间接实现）的所有接口中查找同签名且声明了映射注解的方法
	 *
	 * @param method 处理器方法
	 * @param handlerType 处理器类型
	 * @return 声明了 {@code @RequestMapping} 组合注解的接口方法；未找到时返回 {@code null}
	 */
	private Method findMappedInterfaceMethod(Method method, Class<?> handlerType) {

		Set<Class<?>> interfaces = ClassUtils.getAllInterfacesForClassAsSet(handlerType);
		for (Class<?> interfaceClass : interfaces) {

			try {

				Method candidate = interfaceClass.getDeclaredMethod(method.getName(), method.getParameterTypes());
				if (AnnotatedElementUtils.hasAnnotation(candidate, RequestMapping.class)) {
					return candidate;
				}

			} catch (NoSuchMethodException ignored) {
				// 当前接口未声明同签名方法，继续检查下一个接口
			}

		}

		return null;

	}

}
