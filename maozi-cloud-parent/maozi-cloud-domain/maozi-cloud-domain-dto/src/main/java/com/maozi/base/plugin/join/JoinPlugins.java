package com.maozi.base.plugin.join;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 多 JOIN 关联查询插件注解
 * <p>
 * 当一个实体需要参与多个 JOIN 关联时使用此注解，
 * 可包含多个 {@link JoinPlugin} 配置。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface JoinPlugins {

	/** JOIN 关联配置数组 */
	JoinPlugin [] value();

}
