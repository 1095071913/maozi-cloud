package com.maozi.base.plugin.impl.join;

import com.github.yulichang.wrapper.MPJLambdaWrapper;
import com.maozi.base.plugin.BaseJoinPlugin;
import com.maozi.base.plugin.join.JoinPlugin;
import com.maozi.common.ObjectUtil;
import org.apache.commons.lang3.StringUtils;

/**
 * 左连接（LEFT JOIN）插件
 * <p>
 * 实现左连接的 SQL 拼接逻辑，校验关联表名和连接条件不能为空，
 * 支持表别名设置。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
public class QueryLeftJoinPlugin extends BaseJoinPlugin {

    /** 左联目标为空时的异常提示后缀 */
    private static final String JOIN_TARGET_ERROR = "左联目标";

    /** 左联条件为空时的异常提示后缀 */
    private static final String JOIN_CONDITION_ERROR = "左联条件";

    /**
     * 应用左连接条件
     *
     * @param abbreviationModelName 模型简称
     * @param wrapper MyBatis-Plus-Join Lambda 包装器
     * @param joinPlugin 关联注解实例
     */
    @Override
    public void apply(String abbreviationModelName, MPJLambdaWrapper<?> wrapper, JoinPlugin joinPlugin) {

        String tableName = joinPlugin.tableName();
        ObjectUtil.checkConditionThrowError(StringUtils.isNotEmpty(tableName),
            abbreviationModelName + JOIN_TARGET_ERROR);

        String on = joinPlugin.on();
        ObjectUtil.checkConditionThrowError(StringUtils.isNotEmpty(on),
            abbreviationModelName + JOIN_CONDITION_ERROR);

        // 配置了表别名时拼接为 "表名 as 别名"
        String tableAlias = joinPlugin.tableAlias();
        if(StringUtils.isNotEmpty(tableAlias)){
            tableName += SQL_AS + tableAlias;
        }

        wrapper.leftJoin(tableName + SQL_ON + on);

    }

}
