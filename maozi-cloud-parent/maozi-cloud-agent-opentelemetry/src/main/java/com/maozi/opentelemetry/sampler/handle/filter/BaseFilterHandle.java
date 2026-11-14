package com.maozi.opentelemetry.sampler.handle.filter;

import io.opentelemetry.api.common.Attributes;

import java.util.List;

/**
 * Span 属性过滤器抽象基类
 * <p>
 * 定义按 Span 属性（{@link Attributes}）判断是否需要丢弃该 Span 的统一契约。
 * 子类按来源（HTTP、Redis、MySQL 等）实现具体的过滤规则。
 * </p>
 *
 * @author pengjinlong
 * @date 2026/07/04
 */
public abstract class BaseFilterHandle {

     /** Span 属性键：数据库来源类型（如 mysql、redis） */
     protected static final String ATTR_DB_SYSTEM = "db.system";

     /** Span 属性键：数据库执行语句 */
     protected static final String ATTR_DB_STATEMENT = "db.statement";

     /**
      * 返回该过滤器关注的匹配标识列表，语义因来源类型而异：
      * 来源型过滤器（如 Redis、MySQL）返回来源标识（如 "redis"、"mysql"），
      * 用于匹配 Span 属性中的来源字段；HTTP 过滤器返回需排除的 URL 路径模式（如 "/actuator/**"）。
      *
      * @return 匹配标识列表
      */
     public abstract List<String> getNames();

     /**
      * 根据 Span 属性判断该 Span 是否需要被过滤（丢弃）。
      *
      * @param attributes Span 携带的属性集合
      * @return {@code true} 表示该 Span 命中过滤规则应丢弃；{@code false} 表示保留
      */
     public abstract Boolean filter(Attributes attributes);

}
