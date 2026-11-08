package com.maozi.oauth.token.config.repository;

import com.maozi.base.enums.Status;
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
public class JdbcRegisteredClientRepository extends org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository {

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

	/** 基础查询SQL，以WHERE结尾，后续直接拼接过滤条件 */
	private static final String LOAD_SQL = "SELECT " + COLUMN_NAMES + " FROM " + TABLE_NAME + " WHERE ";

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
		return findByWithStatus("id = ?", id);
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
		return findByWithStatus("client_id = ?", clientId);
	}

	/**
	 * 带状态过滤条件的客户端查询方法
	 * <p>
	 * 在指定的过滤条件基础上，追加状态为启用的条件，只返回启用状态的客户端。
	 * </p>
	 *
	 * @param filter SQL过滤条件（如 "id = ?" 或 "client_id = ?"）
	 * @param args   过滤条件的参数值
	 * @return 匹配的已注册客户端信息，如果不存在则返回null
	 */
	private RegisteredClient findByWithStatus(String filter, Object... args) {
		String sql = LOAD_SQL + filter + " AND status = " + Status.ENABLE.getValue();
		List<RegisteredClient> result = getJdbcOperations().query(
				sql, getRegisteredClientRowMapper(), args);
		return !result.isEmpty() ? result.get(0) : null;
	}

}
