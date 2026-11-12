package com.maozi.base.plugin.query;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 查询插件注解
 * <p>
 * 标注在查询参数的字段上，声明该字段参与查询条件构建的方式。
 * 支持等于、模糊查询、范围查询等多种查询类型，并可指定关联表名和字段名。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface QueryPlugin {

	/** 查询字段名，为空时使用被注解字段的名称 */
	String field() default "";

	/** 查询类型 */
	QueryBaseType value();

	/** 是否为嵌套查询条件 */
	boolean nest() default false;

	/** 关联表名 */
	String tableName() default "";

}
