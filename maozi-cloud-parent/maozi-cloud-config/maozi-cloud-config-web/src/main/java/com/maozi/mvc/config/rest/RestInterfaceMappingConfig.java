package com.maozi.mvc.config.rest;

import org.springframework.boot.autoconfigure.web.servlet.WebMvcRegistrations;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * REST 接口映射注册配置
 * <p>
 * 项目中部分 REST 端点直接继承基类 {@code BaseServiceResultImpl} 的公共实现，
 * 默认映射解析无法关联接口上声明的路由注解（404）。通过 {@link WebMvcRegistrations} 将默认的
 * {@link RequestMappingHandlerMapping} 替换为 {@link InterfaceMethodRequestMappingHandlerMapping}，
 * 支持以接口声明路由、以父类继承实现提供处理器。
 * </p>
 *
 * @author maozi
 * @date 2026/11/13
 */
@Configuration
public class RestInterfaceMappingConfig implements WebMvcRegistrations {

	/**
	 * 获取自定义的请求映射处理器
	 *
	 * @return 支持接口声明路由、父类继承实现的请求映射处理器
	 */
	@Override
	public RequestMappingHandlerMapping getRequestMappingHandlerMapping() {
		return new InterfaceMethodRequestMappingHandlerMapping();
	}

}
