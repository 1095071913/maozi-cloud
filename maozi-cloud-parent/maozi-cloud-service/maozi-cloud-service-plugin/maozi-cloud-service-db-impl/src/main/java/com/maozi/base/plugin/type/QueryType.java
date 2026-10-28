package com.maozi.base.plugin.type;

import com.maozi.base.plugin.QueryBasePlugin;
import com.maozi.base.plugin.impl.query.QueryEqPlugin;
import com.maozi.base.plugin.impl.query.QueryGePlugin;
import com.maozi.base.plugin.impl.query.QueryInPlugin;
import com.maozi.base.plugin.impl.query.QueryLePlugin;
import com.maozi.base.plugin.impl.query.QueryLikePlugin;
import com.maozi.base.plugin.impl.query.QueryNePlugin;
import com.maozi.base.plugin.query.QueryBaseType;
import lombok.Getter;

/**
 * 查询类型枚举
 * <p>
 * 定义支持的查询条件类型及其对应的插件实现。
 * 包括：等于（EQ）、模糊匹配（LIKE）、IN 查询、不等于（NE）、
 * 大于等于（GE）、小于等于（LE）六种查询类型。
 * </p>
 *
 * @author maozi
 */
@Getter
public enum QueryType {

    /** 等于 */
    EQ(QueryBaseType.EQ, new QueryEqPlugin()),

    /** 全模糊匹配 */
    LIKE(QueryBaseType.LIKE, new QueryLikePlugin()),

    /** IN 查询 */
    IN(QueryBaseType.IN, new QueryInPlugin()),

    /** 不等于 */
    NE(QueryBaseType.NE, new QueryNePlugin()),

    /** 大于等于 */
    GE(QueryBaseType.GE, new QueryGePlugin()),

    /** 小于等于 */
    LE(QueryBaseType.LE, new QueryLePlugin()),

    ;

    /** 查询基础类型 */
    private final QueryBaseType type;

    /** 查询插件实例 */
    private final QueryBasePlugin queryPlugin;

    /**
     * 构造方法
     *
     * @param type 查询基础类型
     * @param queryPlugin 查询插件实例
     */
    QueryType(QueryBaseType type,QueryBasePlugin queryPlugin) {

        this.type = type;

        this.queryPlugin = queryPlugin;

    }

    /**
     * 根据类型字符串获取查询类型枚举
     *
     * @param type 类型字符串
     * @return 对应的查询类型枚举，未匹配时返回 null
     */
    public static QueryType get(String type) {

        QueryType[] queryTypes = QueryType.values();

        for(QueryType queryType : queryTypes) {

            if(queryType.getType().getType().equals(type)) {
                return queryType;
            }

        }

        return null;

    }

}
