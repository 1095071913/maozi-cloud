package com.maozi.oauth.token.config.repository;

import com.maozi.base.enums.StatusEnum;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;

/**
 * 基于JDBC的已注册客户端仓库
 * <p>
 * 继承自Spring Authorization Server的JdbcRegisteredClientRepository，
 * 扩展了查询逻辑，在查找客户端时增加了状态过滤条件，只查询启用状态的客户端。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Service
public class JdbcRegisteredClientRepository
        extends org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository {

    /** 数据库查询的列名列表 */
    private static final String COLUMN_NAMES = "id, "
            + "client_id, "
            + "client_id_issued_at, "
            + "client_secret, "
            + "client_secret_expires_at, "
            + "client_name, "
            + "client_authentication_methods, "
            + "authorization_grant_types, "
            + "redirect_uris, "
            + "post_logout_redirect_uris, "
            + "scopes, "
            + "client_settings, "
            + "token_settings";

    /** 数据库表名 */
    private static final String TABLE_NAME = "oauth2_registered_client";

    /** 基础查询SQL，以WHERE结尾，供下方静态查询SQL常量复用 */
    private static final String LOAD_SQL = "SELECT " + COLUMN_NAMES + " FROM " + TABLE_NAME + " WHERE ";

    /** 按主键查询启用状态客户端的 SQL */
    private static final String FIND_BY_ID_SQL = LOAD_SQL + "id = ? AND status = ?";

    /** 按客户端ID查询启用状态客户端的 SQL */
    private static final String FIND_BY_CLIENT_ID_SQL = LOAD_SQL + "client_id = ? AND status = ?";

    /**
     * 构造函数
     *
     * @param jdbcOperations JDBC操作对象，用于执行数据库查询
     */
    public JdbcRegisteredClientRepository(JdbcOperations jdbcOperations) {
        super(jdbcOperations);
    }

    /**
     * 根据ID查找已注册的客户端（仅查询启用状态的客户端）
     *
     * @param id 客户端唯一标识
     * @return 已注册的客户端信息，如果不存在则返回null
     */
    @Override
    public RegisteredClient findById(String id) {
        Assert.hasText(id, "id cannot be empty");
        return findByEnabled(FIND_BY_ID_SQL, id);
    }

    /**
     * 根据客户端ID查找已注册的客户端（仅查询启用状态的客户端）
     *
     * @param clientId 客户端ID
     * @return 已注册的客户端信息，如果不存在则返回null
     */
    @Override
    public RegisteredClient findByClientId(String clientId) {
        Assert.hasText(clientId, "clientId cannot be empty");
        return findByEnabled(FIND_BY_CLIENT_ID_SQL, clientId);
    }

    /**
     * 查询启用状态的客户端
     * <p>
     * 执行传入的静态 SQL 常量（类内定义，非动态拼接），
     * 查询条件值与状态值均通过 {@code ?} 占位符参数化绑定，
     * 无 SQL 注入风险。
     * </p>
     *
     * @param sql 静态查询 SQL 常量（含查询值与状态两个 ? 占位符）
     * @param arg 查询条件值（客户端唯一标识或客户端ID）
     * @return 匹配的已注册客户端信息，如果不存在则返回null
     */
    private RegisteredClient findByEnabled(String sql, Object arg) {
        List<RegisteredClient> result = super.getJdbcOperations().query(
                sql, super.getRegisteredClientRowMapper(), arg, StatusEnum.ENABLE.getValue());
        return !result.isEmpty() ? result.get(0) : null;
    }

}
