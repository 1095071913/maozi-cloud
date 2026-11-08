package com.maozi.base.plugin;

import com.github.yulichang.wrapper.MPJLambdaWrapper;

/**
 * 查询条件插件基类
 * <p>
 * 定义查询条件的应用接口，由具体的查询条件实现类（EQ、LIKE、IN、NE、GE、LE）
 * 分别实现不同的 SQL 条件拼接逻辑。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
public abstract class QueryBasePlugin {

    /**
     * 将查询条件应用到 Wrapper 中
     *
     * @param wrapper MyBatis-Plus-Join Lambda 包装器
     * @param field 字段名
     * @param data 条件值
     */
    public abstract void apply(MPJLambdaWrapper<?> wrapper,String field,Object data);

}
