package com.maozi.db.handler;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.maozi.common.JacksonUtil;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;

/**
 * 通用 JSON 字段处理器
 * <p>
 * 继承 MyBatis-Plus {@link com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler}，通过反射获取字段泛型类型，
 * 自动完成数据库 JSON 列与 Java 对象的双向转换。
 * 支持任意类型：普通对象、List、Map 等。
 * </p>
 * <p>
 * 使用方式：在实体字段上标注 {@code @TableField(typeHandler = JacksonTypeHandler.class)}，
 * 实体类需设置 {@code @TableName(autoResultMap = true)}。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Slf4j
public class JacksonTypeHandler extends com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler {

    /**
     * 仅类型构造（MyBatis-Plus autoResultMap 使用）
     *
     * @param type 字段类型
     */
    public JacksonTypeHandler(Class<?> type) {
        super(type);
    }

    /**
     * 类型 + Field 构造（MyBatis-Plus 通过此构造获取完整泛型信息）
     *
     * @param type  字段类型
     * @param field 字段反射对象（用于解析泛型类型）
     */
    public JacksonTypeHandler(Class<?> type, Field field) {
        super(type, field);
    }

    /**
     * 将 JSON 字符串解析为 Java 对象
     * <p>
     * 通过 {@link #getFieldType()} 获取字段的完整泛型类型（包括 List&lt;Long&gt; 等），
     * 使用 Jackson 进行精确反序列化。
     * </p>
     *
     * @param json JSON 字符串
     * @return Java 对象
     */
    @Override
    public Object parse(String json) {
        // 获取全局共享的 ObjectMapper 实例，保证序列化配置一致性
        ObjectMapper objectMapper = JacksonUtil.getObjectMapper();
        TypeFactory typeFactory = objectMapper.getTypeFactory();
        // 通过 getFieldType() 获取字段的完整泛型类型（如 List<Long>），确保反序列化后类型精确匹配
        JavaType javaType = typeFactory.constructType(this.getFieldType());

        try {
            return objectMapper.readValue(json, javaType);
        } catch (JacksonException e) {
            log.error("反序列化 JSON: {} -> {} 失败", json, javaType, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 将 Java 对象序列化为 JSON 字符串
     *
     * @param obj Java 对象
     * @return JSON 字符串
     */
    @Override
    public String toJson(Object obj) {
        try {
            return JacksonUtil.getObjectMapper().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("序列化对象: {} -> JSON 失败", obj, e);
            throw new RuntimeException(e);
        }
    }

}
