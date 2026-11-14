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
 * MySQL Span 过滤器
 * <p>
 * 当 Span 属性 {@code db.system} 为 {@code mysql} 时，根据 {@code db.statement}
 * 匹配需要排除的 SQL 语句（如连接探活、版本查询、锁检测、information_schema 查询等），
 * 命中则丢弃对应 Span，避免连接池和框架内部维护 SQL 产生噪声链路数据。
 * </p>
 *
 * @author pengjinlong
 * @date 2026/07/04
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MysqlFilterHandle extends BaseFilterHandle {

    /** 来源标识，匹配 Span 属性 {@code db.system} */
    private List<String> names = List.of("mysql");

    /** 需要排除的 SQL 语句列表（连接探活、版本查询、锁检测等框架内部维护 SQL） */
    private final List<String> EXCLUDED_STATEMENT = List.of(

            "SELECT ?",
            "SELECT version()",
            "SELECT DATABASE()",
            "SELECT GET_LOCK(?,?)",
            "SELECT RELEASE_LOCK(?)",

            "SELECT @@sql_safe_updates",
            "SELECT @@foreign_key_checks",
            "SELECT @@GLOBAL.ENFORCE_GTID_CONSISTENCY",

            "SELECT SUBSTRING_INDEX(USER(),?,?)",
            "SET foreign_key_checks=?, sql_safe_updates=?",
            "SELECT SCHEMA_NAME FROM information_schema.SCHEMATA WHERE SCHEMA_NAME = ?",
            "SELECT COUNT(?) FROM information_schema.schemata WHERE schema_name=? LIMIT ?",
            "select VARIABLE_VALUE from performance_schema.global_variables where variable_name = ?",
            "SELECT variable_name FROM performance_schema.user_variables_by_thread WHERE variable_value IS NOT NULL"

    );

    /**
     * 根据 Span 属性判断 MySQL Span 是否需要过滤
     * <p>
     * 先校验 {@code db.system} 是否为 mysql，再比对 {@code db.statement}
     * 是否命中排除语句列表。
     * </p>
     *
     * @param attributes Span 携带的属性集合
     * @return {@code true} 表示命中排除语句应丢弃；{@code false} 表示保留
     */
    @Override
    public Boolean filter(Attributes attributes) {

        // 读取 db.system 来源标识，非 mysql 来源不过滤
        String type = attributes.get(InternalAttributeKeyImpl.create(ATTR_DB_SYSTEM, AttributeType.STRING));
        if(Objects.isNull(type) || !getNames().contains(type)){
            return false;
        }

        // 比对 db.statement 是否命中排除语句列表（属性缺失时不过滤，避免不可变集合 contains(null) 抛 NPE）
        String statement = attributes.get(InternalAttributeKeyImpl.create(ATTR_DB_STATEMENT, AttributeType.STRING));
        if(Objects.isNull(statement)){
            return false;
        }
        return EXCLUDED_STATEMENT.contains(statement);

    }

}
