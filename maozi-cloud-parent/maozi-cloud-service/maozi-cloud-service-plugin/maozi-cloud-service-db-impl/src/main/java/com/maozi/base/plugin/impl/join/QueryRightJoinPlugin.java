package com.maozi.base.plugin.impl.join;

import com.github.yulichang.wrapper.MPJLambdaWrapper;
import com.maozi.base.plugin.BaseJoinPlugin;

/**
 * 右连接（RIGHT JOIN）插件
 * <p>
 * 公共校验与 SQL 拼接逻辑见 {@link BaseJoinPlugin}，
 * 本类仅声明右连接的异常标识与拼接方式。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
public class QueryRightJoinPlugin extends BaseJoinPlugin {

    /** 关联方式名称（用于异常信息标识） */
    private static final String JOIN_NAME = "右联";

    /**
     * 获取关联方式名称，用于异常信息标识
     *
     * @return 右联
     */
    @Override
    protected String joinName() {
        return JOIN_NAME;
    }

    /**
     * 应用右连接 SQL
     *
     * @param wrapper MyBatis-Plus-Join Lambda 包装器
     * @param joinSql 拼接好的关联 SQL
     */
    @Override
    protected void doJoin(MPJLambdaWrapper<?> wrapper, String joinSql) {
        wrapper.rightJoin(joinSql);
    }

}
