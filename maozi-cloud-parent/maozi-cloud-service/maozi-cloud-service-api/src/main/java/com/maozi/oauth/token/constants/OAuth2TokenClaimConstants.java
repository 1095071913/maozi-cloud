package com.maozi.oauth.token.constants;


/**
 * OAuth2令牌内省声明（Claim）字段常量
 * <p>
 * 定义令牌签发及内省响应中使用的声明字段名称常量。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
public final class OAuth2TokenClaimConstants {

	/** 令牌是否活跃 */
	public static final String ACTIVE = "active";

	/** 令牌主体（用户名） */
	public static final String SUB = "sub";

	/** 令牌类型 */
	public static final String TOKEN_TYPE = "token_type";

	/** 用户ID */
	public static final String USER_ID = "user_id";

	/** 客户端ID（已注册客户端 registered client 的主键） */
	public static final String CLIENT_ID = "client_id";

	/** 用户权限列表 */
	public static final String AUTHORITIES = "authorities";

	/** 令牌签发者 */
	public static final String ISS = "iss";

	/** 常量类私有构造，禁止实例化 */
	private OAuth2TokenClaimConstants() {
	}

}
