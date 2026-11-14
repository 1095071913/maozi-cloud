package com.maozi.opentelemetry.sampler.handle.filter.impl;

import com.maozi.opentelemetry.sampler.handle.filter.BaseFilterHandle;
import io.opentelemetry.api.common.AttributeType;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.internal.InternalAttributeKeyImpl;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Objects;

/**
 * Redis Span 过滤器
 * <p>
 * 当 Span 属性 {@code db.system} 为 {@code redis} 时，根据 {@code db.statement}
 * 匹配需要排除的 Redis 指令（如 {@code PING}、{@code AUTH}、{@code INFO server} 等），
 * 命中则丢弃对应 Span，避免连接保活和认证指令产生噪声链路数据。
 * </p>
 *
 * @author pengjinlong
 * @date 2026/07/04
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RedisFilterHandle extends BaseFilterHandle {

    /** 来源标识，匹配 Span 属性 {@code db.system} */
    private final List<String> names = List.of("redis");

    /** 需要排除的 Redis 指令列表（连接保活、认证等指令） */
    private final List<String> EXCLUDED_STATEMENT = List.of(
            "PING",
            "AUTH ?",
            "INFO server",
            "HELLO 3 AUTH ? ?"
    );

    /**
     * 根据 Span 属性判断 Redis Span 是否需要过滤
     * <p>
     * 先校验 {@code db.system} 是否为 redis，再比对 {@code db.statement}
     * 是否命中排除指令列表。
     * </p>
     *
     * @param attributes Span 携带的属性集合
     * @return {@code true} 表示命中排除指令应丢弃；{@code false} 表示保留
     */
    @Override
    public Boolean filter(Attributes attributes) {

        // 读取 db.system 来源标识，非 redis 来源不过滤
        String type = attributes.get(InternalAttributeKeyImpl.create(ATTR_DB_SYSTEM, AttributeType.STRING));
        if(Objects.isNull(type) || !getNames().contains(type)){
            return false;
        }

        // 比对 db.statement 是否命中排除指令列表（属性缺失时不过滤，避免不可变集合 contains(null) 抛 NPE）
        String statement = attributes.get(InternalAttributeKeyImpl.create(ATTR_DB_STATEMENT, AttributeType.STRING));
        if(Objects.isNull(statement)){
            return false;
        }
        return EXCLUDED_STATEMENT.contains(statement);

    }

}
