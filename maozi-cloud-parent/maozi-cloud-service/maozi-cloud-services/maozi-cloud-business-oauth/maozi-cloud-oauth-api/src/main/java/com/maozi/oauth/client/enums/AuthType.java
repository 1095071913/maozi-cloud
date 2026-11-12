package com.maozi.oauth.client.enums;

import cn.hutool.core.util.ObjectUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.maozi.common.enums.BaseEnum;
import lombok.Getter;


/**
 * 授权类型枚举
 * <p>
 * 定义OAuth2.0中支持的授权模式类型，包括授权码模式、客户端模式、刷新令牌模式和密码模式。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
public enum AuthType implements BaseEnum {

	/** 授权码模式 - 适用于有服务端的Web应用，通过授权码换取令牌 */
	AUTHORIZATION_CODE(0, "授权码模式", "authorization_code"),

	/** 客户端模式 - 适用于机器对机器的通信，客户端直接使用自己的凭证获取令牌 */
	CLIENT_CREDENTIALS(1, "客户端模式", "client_credentials"),

	/** 刷新令牌模式 - 使用刷新令牌获取新的访问令牌，避免用户重复授权 */
	REFRESH_TOKEN(2, "刷新令牌模式", "refresh_token"),

	/** 密码模式 - 用户直接提供用户名和密码给客户端，由客户端换取令牌 */
	PASSWORD(3, "密码模式", "password"),

	;
	
	/**
	 * 枚举构造函数
	 *
	 * @param value 枚举值，用于标识授权类型的唯一编号
	 * @param desc  枚举描述，授权类型的中文名称
	 * @param auth  授权类型标识，对应OAuth2.0协议中的grant_type参数值
	 */
	AuthType(Integer value,String desc,String auth) {
		
		this.value = value;
		
		this.desc = desc;
		
		this.auth = auth;
		
	}
	
	/** 枚举值，授权类型的唯一编号 */
	@Getter
	private final Integer value;

	/** 枚举描述，授权类型的中文名称 */
	@Getter
	private final String desc;

	/** 授权类型标识，对应OAuth2.0协议中的grant_type参数值，序列化时忽略 */
	@Getter
	@JsonIgnore
	private final String auth;

	/**
	 * 重写toString方法，返回"枚举值.描述"格式的字符串
	 *
	 * @return 格式为"值.描述"的字符串，例如"0.授权码模式"
	 */
	@Override
	public String toString() {
		return value + "." + desc;
	}

	/**
	 * 根据授权类型标识字符串获取对应的枚举实例
	 *
	 * @param auth OAuth2.0协议中的grant_type参数值，如"authorization_code"
	 * @return 对应的AuthType枚举实例，未找到则返回null
	 */
	public static AuthType getByAuth(String auth) {

		if(ObjectUtil.isNull(auth)){
			return null;
		}

		AuthType[] values = values();
		for (AuthType authType : values) {
			if(authType.auth.equals(auth)){
				return authType;
			}
		}
		return null;
	}
	
}
