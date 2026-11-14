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
public final class OAuth2TokenUserClaimConstants {

	/** 用户ID */
	public static final String USER_ID = "user_id";

	/** 用户权限列表 */
	public static final String AUTHORITIES = "authorities";

	/** 常量类私有构造，禁止实例化 */
	private OAuth2TokenUserClaimConstants() {}

}
