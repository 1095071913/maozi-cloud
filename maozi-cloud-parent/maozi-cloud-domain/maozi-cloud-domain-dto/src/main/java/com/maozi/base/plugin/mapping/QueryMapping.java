package com.maozi.base.plugin.mapping;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 查询映射注解
 * <p>
 * 标注在结果对象的字段上，声明查询结果的映射配置。
 * 支持远程服务调用映射、关联字段映射以及忽略映射等功能。
 * </p>
 * <p>
 * 使用场景：
 * <ul>
 *   <li>标记字段为远程服务数据来源（isService=true + serviceName）</li>
 *   <li>标记字段来自关联表的某个字段（tableName + field）</li>
 *   <li>标记字段需要通过自定义函数处理映射（functionName）</li>
 *   <li>忽略某个字段的自动映射（ignore=true）</li>
 * </ul>
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface QueryMapping {

	/** 是否通过远程服务获取数据 */
	boolean isService() default false;

	/** 远程服务名称 */
	String serviceName() default "";

	/** 关联字段名 */
	String relationField() default "";

	/** 是否忽略该字段的映射 */
	boolean ignore() default false;

	/** 映射处理函数名称 */
	String functionName() default "";

	/** 来源表名 */
	String tableName() default "";

	/** 来源字段名 */
	String field() default "";

}
