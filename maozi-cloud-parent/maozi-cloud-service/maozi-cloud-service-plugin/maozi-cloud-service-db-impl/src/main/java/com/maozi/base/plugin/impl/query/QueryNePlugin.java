package com.maozi.base.plugin.impl.query;

import com.github.yulichang.wrapper.MPJLambdaWrapper;
import com.maozi.base.plugin.BaseQueryPlugin;

/**
 * 不等于查询条件插件
 * <p>
 * 实现 {@code WHERE field <> data} 的 SQL 条件拼接。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
public class QueryNePlugin extends BaseQueryPlugin{

    /**
     * 应用不等于条件
     *
     * @param wrapper MyBatis-Plus-Join Lambda 包装器
     * @param field 字段名
     * @param data 条件值
     */
    @Override
    public void apply(MPJLambdaWrapper<?> wrapper,String field,Object data) {
        wrapper.ne(field, data);
    }

}
