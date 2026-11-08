package com.maozi.base.plugin.type;

import com.maozi.base.plugin.JoinBasePlugin;
import com.maozi.base.plugin.impl.join.QueryInnerJoinPlugin;
import com.maozi.base.plugin.impl.join.QueryLeftJoinPlugin;
import com.maozi.base.plugin.impl.join.QueryRightJoinPlugin;
import com.maozi.base.plugin.join.JoinBaseType;
import lombok.Getter;

/**
 * 关联类型枚举
 * <p>
 * 定义支持的关联查询类型及其对应的插件实现。
 * 包括：内连接（INNER JOIN）、左连接（LEFT JOIN）、右连接（RIGHT JOIN）三种关联类型。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
@Getter
public enum JoinType {

    /** 内连接 */
    INNER_JOIN(JoinBaseType.INNER_JOIN, new QueryInnerJoinPlugin()),

    /** 左连接 */
    LEFT_JOIN(JoinBaseType.LEFT_JOIN, new QueryLeftJoinPlugin()),

    /** 右连接 */
    RIGHT_JOIN(JoinBaseType.RIGHT_JOIN, new QueryRightJoinPlugin()),

    ;

    /** 关联基础类型 */
    private final JoinBaseType type;

    /** 关联插件实例 */
    private final JoinBasePlugin joinPlugin;

    /**
     * 构造方法
     *
     * @param type 关联基础类型
     * @param joinPlugin 关联插件实例
     */
    JoinType(JoinBaseType type, JoinBasePlugin joinPlugin) {

        this.type = type;

        this.joinPlugin = joinPlugin;

    }

    /**
     * 根据类型字符串获取关联类型枚举
     *
     * @param type 类型字符串
     * @return 对应的关联类型枚举，未匹配时返回 null
     */
    public static JoinType get(String type) {

        JoinType[] joinTypes = JoinType.values();

        for(JoinType joinType : joinTypes) {

            if(joinType.getType().getType().equals(type)) {
                return joinType;
            }

        }

        return null;

    }

}
