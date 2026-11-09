package com.maozi.oauth.client.handler;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.maozi.db.handler.JacksonTypeHandler;
import com.maozi.oauth.client.api.impl.ClientServiceImpl;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;

/**
 * 专用于 OAuth2 Settings 字段的 JSON 类型处理器。
 * <p>
 * 使用注册了 Spring Security OAuth2 Jackson 模块的 ObjectMapper，
 * 正确处理数据库中 token_settings / client_settings 的类型感知 JSON 格式
 * （如 ["java.time.Duration", 7200.000000000]、{"@class":"...OAuth2TokenFormat","value":"reference"} 等）。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
@Slf4j
public class OAuth2SettingsTypeHandler extends JacksonTypeHandler {

    /** OAuth2专用的ObjectMapper，复用ClientServiceImpl中已注册安全模块的实例 */
    private static final ObjectMapper OAUTH2_OBJECT_MAPPER = ClientServiceImpl.OAUTH_OBJECT_MAPPER;

    /**
     * 构造方法，仅指定类型。
     *
     * @param type 字段类型
     */
    public OAuth2SettingsTypeHandler(Class<?> type) {
        super(type);
    }

    /**
     * 构造方法，指定类型和字段。
     *
     * @param type  字段类型
     * @param field 字段反射对象
     */
    public OAuth2SettingsTypeHandler(Class<?> type, Field field) {
        super(type, field);
    }

    /**
     * 将JSON字符串反序列化为OAuth2设置对象。
     * <p>
     * 使用OAuth2专用的ObjectMapper，支持Spring Security OAuth2的类型感知JSON格式。
     * </p>
     *
     * @param json 数据库中存储的JSON字符串
     * @return 反序列化后的设置对象（Map类型）
     * @throws RuntimeException JSON解析失败时抛出
     */
    @Override
    public Object parse(String json) {
        TypeFactory typeFactory = OAUTH2_OBJECT_MAPPER.getTypeFactory();
        JavaType javaType = typeFactory.constructType(this.getFieldType());
        try {
            return OAUTH2_OBJECT_MAPPER.readValue(json, javaType);
        } catch (JacksonException e) {
            log.error("反序列化 OAuth2 Settings JSON: {} -> {} 失败", json, javaType, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 将OAuth2设置对象序列化为JSON字符串。
     * <p>
     * 使用OAuth2专用的ObjectMapper，确保序列化结果包含类型信息。
     * </p>
     *
     * @param obj 待序列化的设置对象
     * @return 序列化后的JSON字符串
     * @throws RuntimeException JSON序列化失败时抛出
     */
    @Override
    public String toJson(Object obj) {
        try {
            return OAUTH2_OBJECT_MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("序列化 OAuth2 Settings 对象: {} -> JSON 失败", obj, e);
            throw new RuntimeException(e);
        }
    }

}
