package com.maozi.base.plugin;

import com.github.yulichang.wrapper.MPJLambdaWrapper;
import com.maozi.base.plugin.join.JoinPlugin;
import com.maozi.common.ObjectUtil;
import org.apache.commons.lang3.StringUtils;

/**
 * 关联查询插件基类
 * <p>
 * 以模板方法封装 JOIN 关联查询的通用逻辑：校验关联表名和连接条件不能为空、
 * 支持表别名设置，并将拼接好的关联 SQL 交给具体实现类（InnerJoin、LeftJoin、
 * RightJoin）应用到 Wrapper 中。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
public abstract class BaseJoinPlugin {

    /** 表别名拼接关键字 */
    protected static final String SQL_AS = " AS ";

    /** 连接条件拼接关键字 */
    protected static final String SQL_ON = " ON ";

    /** 关联目标为空时的异常提示后缀 */
    private static final String JOIN_TARGET_ERROR = "目标";

    /** 关联条件为空时的异常提示后缀 */
    private static final String JOIN_CONDITION_ERROR = "条件";

    /**
     * 将关联查询条件应用到 Wrapper 中
     *
     * @param abbreviationModelName 模型简称，用于异常信息标识
     * @param wrapper MyBatis-Plus-Join Lambda 包装器
     * @param joinPlugin 关联注解实例，包含表名、连接条件和别名等信息
     */
    public void apply(String abbreviationModelName, MPJLambdaWrapper<?> wrapper, JoinPlugin joinPlugin) {

        // 缓存关联方式名称，避免同一方法内重复调用
        String joinName = joinName();

        String tableName = joinPlugin.tableName();
        ObjectUtil.checkConditionThrowError(StringUtils.isNotEmpty(tableName),
            abbreviationModelName + joinName + JOIN_TARGET_ERROR);

        String on = joinPlugin.on();
        ObjectUtil.checkConditionThrowError(StringUtils.isNotEmpty(on),
            abbreviationModelName + joinName + JOIN_CONDITION_ERROR);

        // 配置了表别名时拼接为 "表名 as 别名"
        String tableAlias = joinPlugin.tableAlias();
        if (StringUtils.isNotEmpty(tableAlias)){
            tableName += SQL_AS + tableAlias;
        }

        doJoin(wrapper, tableName + SQL_ON + on);

    }

    /**
     * 获取关联方式名称（内联/左联/右联），用于异常信息标识
     *
     * @return 关联方式名称
     */
    protected abstract String joinName();

    /**
     * 将拼接好的关联 SQL 应用到 Wrapper 中
     *
     * @param wrapper MyBatis-Plus-Join Lambda 包装器
     * @param joinSql 拼接好的关联 SQL，如 "表名 AS 别名 ON 条件"
     */
    protected abstract void doJoin(MPJLambdaWrapper<?> wrapper, String joinSql);

}
