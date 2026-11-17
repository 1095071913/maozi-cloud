package com.maozi.oauth.client.handler;

import com.maozi.common.ObjectUtil;
import com.maozi.db.handler.JacksonTypeHandler;
import com.maozi.oauth.client.enums.AuthTypeEnum;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * OAuth2授权方式类型处理器
 * <p>
 * 用于处理客户端授权方式（AuthTypeEnum）集合与数据库逗号分隔字符串之间的转换。
 * 将数据库中以逗号分隔的授权方式字符串解析为AuthTypeEnum枚举集合，
 * 反之将AuthTypeEnum枚举集合序列化为逗号分隔的字符串存储到数据库。
 * </p>
 *
 * @author pengjinlong
 * @since 2026/6/2 15:19
 */
public class Oauth2AuthTypeHandler extends JacksonTypeHandler {

    /**
     * 构造方法，仅指定类型
     *
     * @param type 字段类型
     */
    public Oauth2AuthTypeHandler(Class<?> type) {
        super(type);
    }

    /**
     * 构造方法，指定类型和字段
     *
     * @param type  字段类型
     * @param field 字段反射对象
     */
    public Oauth2AuthTypeHandler(Class<?> type, Field field) {
        super(type, field);
    }

    /**
     * 将数据库中的逗号分隔字符串解析为AuthTypeEnum枚举集合
     * <p>
     * 按逗号分割字符串，去除空格后通过AuthTypeEnum枚举的getByAuth方法转换为枚举值，
     * 过滤掉无效值后收集为Set集合。
     * </p>
     *
     * @param json 数据库中存储的逗号分隔字符串
     * @return AuthTypeEnum枚举集合
     */
    @Override
    public Object parse(String json) {
        return Arrays.stream(json.split(","))
                .map(String::trim) // 去除空格（容错）
                .map(AuthTypeEnum::getByAuth)
                .filter(ObjectUtil::isNotNullEmpty)
                .collect(Collectors.toSet());
    }

    /**
     * 将AuthTypeEnum枚举集合序列化为逗号分隔的字符串
     * <p>
     * 先进行类型判断和安全强转，只保留AuthTypeEnum类型的元素，
     * 提取每个枚举的auth值，过滤null值后以逗号拼接。
     * </p>
     *
     * @param obj AuthTypeEnum枚举集合
     * @return 逗号分隔的授权方式字符串，类型不匹配时返回null
     */
    @Override
    public String toJson(Object obj) {

        // 1. 类型判断 + 泛型安全强转
        if (!(obj instanceof Set<?> authTypes)) {
            return null;
        }

        return authTypes.stream()
                .filter(AuthTypeEnum.class::isInstance)       // 只保留AuthTypeEnum类型
                .map(AuthTypeEnum.class::cast)                // 安全强转
                .map(AuthTypeEnum::getAuth)                   // 提取getAuth()
                .filter(Objects::nonNull)                 // 过滤null值，避免出现",,,"
                .collect(Collectors.joining(","));        // 逗号拼接

    }

}
