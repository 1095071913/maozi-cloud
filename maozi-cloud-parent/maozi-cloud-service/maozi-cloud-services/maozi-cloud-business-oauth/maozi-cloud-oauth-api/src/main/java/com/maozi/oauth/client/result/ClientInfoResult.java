package com.maozi.oauth.client.result;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.maozi.base.enums.Status;
import com.maozi.oauth.client.enums.AuthType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Set;

/**
 * 客户端详细信息视图对象
 * <p>
 * 用于返回OAuth客户端的详细配置信息，包括客户端ID、名称、
 * 支持的授权模式、令牌有效期、备注及状态等。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Data
public class ClientInfoResult implements Serializable {
    /** 序列化标识 */
	@Serial
    private static final long serialVersionUID = 1L;

	/** 客户端ID，客户端的唯一标识符 */
	@Schema(description = "客户端ID")
	private String clientId;

	/** 客户端名称，用于标识和描述客户端 */
	@Schema(description = "名称")
	private String name;

	/** 授权模式集合，该客户端支持的OAuth2.0授权类型 */
	@Schema(description = "授权模式", ref = "IntegerArrayList")
	private Set<AuthType> authorizationGrantTypes;

	/** 授权令牌（AccessToken）有效期，单位为秒（从TokenSettings解析） */
	@Schema(description = "授权令牌有效期 秒 默认2小时")
	private Long accessTokenValiditySeconds;

	/** 刷新令牌（RefreshToken）有效期，单位为秒（从TokenSettings解析） */
	@Schema(description = "刷新令牌有效期 秒 默认7天")
	private Long refreshTokenValiditySeconds;

	/** 备注信息，记录客户端的额外说明 */
	@Schema(description = "备注")
	private String remark;

	/** 客户端状态，序列化为整数类型 */
	@Schema(description = "状态")
	@JsonFormat(shape = JsonFormat.Shape.NUMBER_INT)
	private Status status;
	
}
