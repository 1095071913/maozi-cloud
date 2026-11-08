package com.maozi.db.constant;

/**
 * 数据库表字段名常量
 * <p>
 * 集中维护跨模块复用的数据库列名，避免 SQL / MyBatis-Plus 查询中硬编码字符串。
 * </p>
 *
 * @author pengjinlong
 * @since 2026/6/16 18:33
 */
public final class TableColumnName {

    /** 私有构造器，防止常量类被实例化 */
    private TableColumnName() {}

    /** 租户标识列名（client_id），被 MyBatis-Plus 多租户插件用作租户隔离字段，兼作 OAuth2 客户端关联字段 */
    public static final String CLIENT_ID = "client_id";

}
