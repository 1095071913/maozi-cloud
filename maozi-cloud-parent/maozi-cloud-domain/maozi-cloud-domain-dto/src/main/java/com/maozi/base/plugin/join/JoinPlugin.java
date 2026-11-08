package com.maozi.base.plugin.join;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * JOIN 关联查询插件注解
 * <p>
 * 标注在类上，声明该实体参与 JOIN 关联查询的配置信息，
 * 包括关联类型（内连接/左连接/右连接）、关联条件和表信息。
 * 同一实体需要配置多个 JOIN 关联时，使用 {@link JoinPlugins} 包装多个本注解。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface JoinPlugin {

	/** 关联类型 */
	JoinBaseType value();

	/** 关联条件，如 "a.id = b.a_id" */
	String on() default "";

	/** 关联表名 */
	String tableName() default "";

	/** 关联表别名 */
	String tableAlias() default "";

}
