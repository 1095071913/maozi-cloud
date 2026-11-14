package com.maozi.base.plugin;

import com.github.yulichang.wrapper.MPJLambdaWrapper;
import com.maozi.base.plugin.join.JoinPlugin;

/**
 * 关联查询插件基类
 * <p>
 * 定义关联查询条件的应用接口，由具体的关联查询实现类
 * （InnerJoin、LeftJoin、RightJoin）分别实现不同的 SQL 关联拼接逻辑。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
public abstract class BaseJoinPlugin {

	/**
	 * 将关联查询条件应用到 Wrapper 中
	 *
	 * @param abbreviationModelName 模型简称，用于异常信息标识
	 * @param wrapper MyBatis-Plus-Join Lambda 包装器
	 * @param joinPlugin 关联注解实例，包含表名、连接条件和别名等信息
	 */
	public abstract void apply(String abbreviationModelName,MPJLambdaWrapper<?> wrapper, JoinPlugin joinPlugin);

}
