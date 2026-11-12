package com.maozi.mvc.config.json;

import cn.hutool.core.util.ClassUtil;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maozi.common.JacksonUtil;
import com.maozi.common.context.ApplicationEnvironmentContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

/**
 * Jackson JSON 序列化配置
 * <p>
 * 配置全局 ObjectMapper 实例，初始化自定义序列化/反序列化规则，
 * 并扫描项目包下所有枚举类统一设置为整数格式输出（{@code JsonFormat.Shape.NUMBER_INT}）。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Configuration
public class JacksonConfig {

	/**
	 * 创建全局 Jackson ObjectMapper
	 *
	 * @param builder Jackson 构建器
	 * @return 配置好的 ObjectMapper 实例
	 */
	@Bean
	public ObjectMapper jacksonObjectMapper(Jackson2ObjectMapperBuilder builder) {

		// 通过 Spring 提供的构建器创建 ObjectMapper（非 XML 模式）
		ObjectMapper objectMapper = builder.createXmlMapper(false).build();

		// 初始化自定义的序列化/反序列化配置（如日期格式、空值处理等）
		return JacksonUtil.initObjectMapperConfig(objectMapper);

	}

	/**
	 * 创建 HTTP 消息转换器，配置枚举类以整数格式序列化
	 *
	 * @param objectMapper 全局 ObjectMapper 实例
	 * @return 配置好的 MappingJackson2HttpMessageConverter
	 */
	@Bean
    public MappingJackson2HttpMessageConverter httpMessageConverter(ObjectMapper objectMapper) {

		// 扫描项目基础包下的所有类，将枚举类统一配置为输出整数值（而非字符串名称）
        ClassUtil.scanPackage(ApplicationEnvironmentContext.PACKAGE_PREFIX).forEach(item -> {
        	if(item.isEnum()) {
        		// 将枚举序列化格式覆盖为 NUMBER_INT，确保前端接收到的是整数值
        		objectMapper.configOverride(item).setFormat(JsonFormat.Value.forShape(JsonFormat.Shape.NUMBER_INT));
        	}
        });

        return new MappingJackson2HttpMessageConverter(objectMapper);

    }

}
