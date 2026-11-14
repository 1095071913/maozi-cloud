package com.maozi.base.plugin.impl.query;

import com.github.yulichang.wrapper.MPJLambdaWrapper;
import com.maozi.base.plugin.BaseQueryPlugin;

/**
 * IN 查询条件插件
 * <p>
 * 实现 {@code WHERE field IN (data)} 的 SQL 条件拼接。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
public class QueryInPlugin extends BaseQueryPlugin {

    /**
     * 应用 IN 条件
     *
     * @param wrapper MyBatis-Plus-Join Lambda 包装器
     * @param field 字段名
     * @param data 条件值（集合）
     */
    @Override
    public void apply(MPJLambdaWrapper<?> wrapper,String field,Object data) {
        wrapper.in(field, data);
    }

}
