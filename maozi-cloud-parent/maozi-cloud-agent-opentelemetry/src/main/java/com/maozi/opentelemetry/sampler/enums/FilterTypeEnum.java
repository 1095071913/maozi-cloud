package com.maozi.opentelemetry.sampler.enums;

import com.maozi.opentelemetry.sampler.handle.filter.BaseFilterHandle;
import com.maozi.opentelemetry.sampler.handle.filter.impl.HttpFilterHandle;
import com.maozi.opentelemetry.sampler.handle.filter.impl.MysqlFilterHandle;
import com.maozi.opentelemetry.sampler.handle.filter.impl.RedisFilterHandle;
import lombok.Getter;

/**
 * Span 属性过滤类型枚举
 * <p>
 * 定义按来源分类的过滤器类型，每种类型绑定一个 {@link BaseFilterHandle} 实现，
 * 用于在 {@link com.maozi.opentelemetry.sampler.config.FilterSampler} 中按 Span 属性判断是否丢弃。
 * </p>
 *
 * @author pengjinlong
 * @date 2026/07/04
 */
public enum FilterTypeEnum {

    /** HTTP 请求来源 */
    HTTP(0,"Http",new HttpFilterHandle()),

    /** Redis 访问来源 */
    REDIS(1,"Redis", new RedisFilterHandle()),

    /** MySQL 访问来源 */
    MYSQL(2,"Mysql",new MysqlFilterHandle()),

    ;


    /**
     * 构造过滤器类型
     *
     * @param value 枚举的整型值
     * @param desc 来源类型的英文描述
     * @param handle 该类型绑定的过滤器实现
     */
    FilterTypeEnum(Integer value, String desc, BaseFilterHandle handle){

        this.value = value;

        this.desc = desc;

        this.handle = handle;

    };

    /** 枚举的整型值 */
    @Getter
    private final Integer value;

    /** 来源类型的英文描述 */
    @Getter
    private final String desc;

    /** 该类型绑定的过滤器实现 */
    @Getter
    private final BaseFilterHandle handle;

}
