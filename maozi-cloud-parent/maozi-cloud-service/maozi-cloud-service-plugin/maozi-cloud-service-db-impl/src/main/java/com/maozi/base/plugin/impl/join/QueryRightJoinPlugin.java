package com.maozi.base.plugin.impl.join;

import com.github.yulichang.wrapper.MPJLambdaWrapper;
import com.maozi.base.plugin.JoinBasePlugin;
import com.maozi.base.plugin.join.JoinPlugin;
import com.maozi.common.ObjectUtil;
import org.apache.commons.lang3.StringUtils;

/**
 * 右连接（RIGHT JOIN）插件
 * <p>
 * 实现右连接的 SQL 拼接逻辑，校验关联表名和连接条件不能为空，
 * 支持表别名设置。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
public class QueryRightJoinPlugin extends JoinBasePlugin {

    /**
     * 应用右连接条件
     *
     * @param abbreviationModelName 模型简称
     * @param wrapper MyBatis-Plus-Join Lambda 包装器
     * @param joinPlugin 关联注解实例
     */
    @Override
    public void apply(String abbreviationModelName,MPJLambdaWrapper<?> wrapper, JoinPlugin joinPlugin) {

        ObjectUtil.isNullEmptyThrowError(joinPlugin.tableName(),abbreviationModelName + "右联目标");

        ObjectUtil.isNullEmptyThrowError(joinPlugin.on(),abbreviationModelName + "右联条件");

        String tableName = joinPlugin.tableName();

        // 配置了表别名时拼接为 "表名 as 别名"
        if(StringUtils.isNotBlank(joinPlugin.tableAlias())){
            tableName += " as " + joinPlugin.tableAlias();
        }

        wrapper.rightJoin(tableName + " on " + joinPlugin.on());

    }

}
