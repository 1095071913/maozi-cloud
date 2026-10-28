package com.maozi.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.deser.std.StringDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/**
 * Jackson JSON 工具类
 * <p>
 * 封装 Jackson {@link ObjectMapper} 的常用操作，包括 JSON 序列化/反序列化、
 * 对象与 Map 的互转等。全局配置了 Long 转字符串（防止前端精度丢失）、
 * 空字符串转 null、Java 8 日期时间支持等特性。
 * </p>
 *
 * @author maozi
 */
@Slf4j
public class JacksonUtil {

	/** 全局 ObjectMapper 实例 */
	@Getter
    private static final ObjectMapper objectMapper;

	// 类加载时初始化全局 ObjectMapper，确保配置只执行一次
	static{
		objectMapper = initObjectMapperConfig();
	}

	/**
	 * 使用默认 ObjectMapper 初始化配置
	 *
	 * @return 配置好的 ObjectMapper
	 */
	public static ObjectMapper initObjectMapperConfig(){
		return initObjectMapperConfig(new ObjectMapper());
	}

	/**
	 * 初始化 ObjectMapper 配置
	 * <p>
	 * 配置包括：非空序列化、东八区时区、日期格式、Long 转字符串、
	 * 空字符串转 null、Java 8 日期时间支持等。
	 * </p>
	 *
	 * @param objectMapper 待配置的 ObjectMapper 实例
	 * @return 配置好的 ObjectMapper，传入为空时返回 null
	 */
	public static ObjectMapper initObjectMapperConfig(ObjectMapper objectMapper){

		if(ObjectUtil.isNullEmpty(objectMapper)){
			return null;
		}

		objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

		objectMapper.setTimeZone(TimeZone.getTimeZone("GMT+8"));
		// java.util.Date 的序列化/反序列化日期格式
		objectMapper.setDateFormat(new java.text.SimpleDateFormat("yyyy‑MM‑dd HH:mm:ss.SSS"));
		objectMapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
		objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
		objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
		objectMapper.configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true);
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		objectMapper.configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true);
		objectMapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);

		SimpleModule module = new SimpleModule();
		// Long 序列化为字符串，防止前端精度丢失
		module.addSerializer(Long.class, ToStringSerializer.instance);
		module.addSerializer(Long.TYPE, ToStringSerializer.instance);
		// 空字符串反序列化为 null
		module.addDeserializer(String.class, new StdDeserializer<>(String.class) {

			/** 先按默认逻辑反序列化字符串，结果为空字符串时归一为 null */
			@Override
			public String deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
				String result = StringDeserializer.instance.deserialize(jsonParser, deserializationContext);
				if (ObjectUtil.isNullEmpty(result)) {
					return null;
				}
				return result;
			}
		});

		// LocalDateTime 自定义序列化/反序列化，格式为 yyyy-MM-dd HH:mm:ss.SSS（空格分隔、含毫秒，不带 T）
		DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy‑MM‑dd HH:mm:ss.SSS");
		module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(dateTimeFormatter));
		module.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(dateTimeFormatter));

		// 注册 JavaTimeModule，支持 Java 8 日期时间类型
		objectMapper.registerModule(new JavaTimeModule());
		// 自定义 SimpleModule 最后注册，确保 LocalDateTime 使用自定义格式而非 ISO-8601（带 T）
		objectMapper.registerModule(module);

		return objectMapper;

	}

	/**
	 * 将对象序列化为 JSON 字符串
	 *
	 * @param data Java 对象
	 * @return JSON 字符串，异常时返回 null
	 */
    public static String objectToJson(Object data){

		try {return objectMapper.writeValueAsString(data);} catch (JsonProcessingException e) {
			log.error(e.getLocalizedMessage(),e);
			return null;
		}

	}

	/**
	 * 将 JSON 字符串反序列化为 Java 对象
	 *
	 * @param jsonString JSON 字符串
	 * @param type 目标类型
	 * @param <T> 目标类型泛型
	 * @return Java 对象，异常时返回 null
	 */
	public static <T> T jsonToObject(String jsonString, Class<T> type){

		try {return objectMapper.readValue(jsonString, type);} catch (JsonProcessingException e) {
			log.error(e.getLocalizedMessage(),e);
			return null;
		}

	}

	/**
	 * 将 JSON 字符串反序列化为 List
	 *
	 * @param json JSON 字符串
	 * @param <T> 列表元素类型
	 * @return List 对象，异常时返回 null
	 */
	public static <T> List<T> jsonToList(String json) {

		TypeReference<List<T>> typeReference = new TypeReference<>() {};

		try {return objectMapper.readValue(json,typeReference);} catch (Exception e) {
			log.error(e.getLocalizedMessage(),e);
			return null;
		}

	}

	/**
	 * 将 JSON 字符串反序列化为 Map
	 *
	 * @param json JSON 字符串
	 * @param <T> Map 值类型
	 * @return Map 对象，异常时返回 null
	 */
	public static <T> Map<String, T> jsonToMap(String json) {

		TypeReference<Map<String, T>> typeReference = new TypeReference<>() {};

		try {return objectMapper.readValue(json,typeReference);} catch (Exception e) {
			log.error(e.getLocalizedMessage(),e);
			return null;
		}

	}

	/**
	 * 将 Java 对象转换为 Map
	 *
	 * @param data Java 对象
	 * @param <T> Map 值类型
	 * @return Map 对象，异常时返回 null
	 */
	public static <T> Map<String, T> objectToMap(Object data) {

		TypeReference<Map<String, T>> typeReference = new TypeReference<>() {};

		try {return objectMapper.convertValue(data,typeReference);} catch (Exception e) {
			log.error(e.getLocalizedMessage(),e);
			return null;
		}

	}

	/**
	 * 将 Map 转换为 Java 对象
	 *
	 * @param data Map 数据
	 * @param type 目标类型
	 * @param <T> 目标类型泛型
	 * @return Java 对象，异常时返回 null
	 */
	public static <T> T mapToObject(Map<String, ?> data, Class<T> type) {

		try {return objectMapper.convertValue(data,type);} catch (Exception e) {
			log.error(e.getLocalizedMessage(),e);
			return null;
		}

	}

	/**
	 * 将 Map 转换为 Java 对象（支持泛型类型）
	 *
	 * @param data Map 数据
	 * @param type 目标类型（含泛型信息）
	 * @param <T> 目标类型泛型
	 * @return Java 对象，异常时返回 null
	 */
	public static <T> T mapToObject(Map<String, ?> data, Type type) {

		JavaType constructType = TypeFactory.defaultInstance().constructType(type);

		try {return objectMapper.convertValue(data,constructType);} catch (Exception e) {
			log.error(e.getLocalizedMessage(),e);
			return null;
		}

	}

	/**
	 * 将 Map 列表转换为对象列表
	 *
	 * @param data Map 列表
	 * @param <T> 目标类型泛型
	 * @return 对象列表，异常时返回 null
	 */
	public static <T> List<T> mapListToListObject(List<Map<String,Object>> data) {

		TypeReference<List<T>> typeReference = new TypeReference<>() {};

		try {return objectMapper.convertValue(data,typeReference);} catch (Exception e) {
			log.error(e.getLocalizedMessage(),e);
			return null;
		}

	}

	/**
	 * 将对象列表转换为 Map 列表
	 *
	 * @param data 对象列表
	 * @return Map 列表，异常时返回 null
	 */
	public static <T> List<Map<String,Object>> listObjectToMapList(List<?> data) {

		TypeReference<List<Map<String,Object>>> typeReference = new TypeReference<>() {};

		try {return objectMapper.convertValue(data,typeReference);} catch (Exception e) {
			log.error(e.getLocalizedMessage(),e);
			return null;
		}

	}

}
