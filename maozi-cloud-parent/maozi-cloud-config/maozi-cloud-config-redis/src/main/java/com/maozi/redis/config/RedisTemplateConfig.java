package com.maozi.redis.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * Redis 模板配置
 * <p>
 * 配置 RedisTemplate 的序列化策略：键和哈希键使用字符串序列化，
 * 值和哈希值使用 Java 原生序列化，确保数据可读性和兼容性。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
@Configuration
public class RedisTemplateConfig {

	/**
	 * 创建 RedisTemplate 实例
	 *
	 * @param factory Redis 连接工厂
	 * @return 配置好序列化器的 RedisTemplate
	 */
	@Bean
	public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {

		RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
		redisTemplate.setConnectionFactory(factory);

		redisTemplate.setKeySerializer(RedisSerializer.string());
		redisTemplate.setHashKeySerializer(RedisSerializer.string());
		redisTemplate.setValueSerializer(RedisSerializer.java());
		redisTemplate.setHashValueSerializer(RedisSerializer.java());

		redisTemplate.afterPropertiesSet();

		return redisTemplate;

	}

}
