package com.maozi.base.plugin.impl.query;

import com.github.yulichang.wrapper.MPJLambdaWrapper;
import com.maozi.base.plugin.QueryBasePlugin;

/**
 * 模糊匹配查询条件插件
 * <p>
 * 实现 {@code WHERE field LIKE '%data%'} 的 SQL 条件拼接（全模糊匹配）。
 * </p>
 *
 * @author maozi
 */
public class QueryLikePlugin extends QueryBasePlugin{

    /**
     * 应用全模糊匹配条件
     *
     * @param wrapper MyBatis-Plus-Join Lambda 包装器
     * @param field 字段名
     * @param data 条件值
     */
    @Override
    public void apply(MPJLambdaWrapper<?> wrapper,String field,Object data) {
        wrapper.like(field, data);
    }

}
