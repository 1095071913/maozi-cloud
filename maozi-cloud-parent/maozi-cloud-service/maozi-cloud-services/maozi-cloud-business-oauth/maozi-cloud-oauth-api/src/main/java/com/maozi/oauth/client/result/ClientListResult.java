package com.maozi.oauth.client.result;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.maozi.base.enums.Status;
import com.maozi.base.plugin.mapping.QueryMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * 客户端列表视图对象
 * <p>
 * 用于OAuth客户端列表页面展示的视图对象，包含客户端的基本信息，
 * 如ID、客户端ID、名称、令牌有效期和状态等。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Data
public class ClientListResult implements Serializable {
    /** 序列化标识 */
	@Serial
    private static final long serialVersionUID = 1L;

	/** 客户端记录的主键ID */
	@Schema(description = "ID")
	private Long id;

	/** 客户端ID，客户端的唯一标识符 */
	@Schema(description = "客户端ID")
	private String clientId;

	/** 客户端名称，用于标识和描述客户端 */
	@Schema(description = "名称")
	private String clientName;

	/** 授权令牌（AccessToken）有效期，单位为秒（从TokenSettings解析），查询映射时忽略 */
	@QueryMapping(ignore = true)
	@Schema(description = "授权令牌有效期 秒 默认2小时")
	private Long accessTokenValiditySeconds;

	/** 刷新令牌（RefreshToken）有效期，单位为秒（从TokenSettings解析），查询映射时忽略 */
	@QueryMapping(ignore = true)
	@Schema(description = "刷新令牌有效期 秒 默认7天")
	private Long refreshTokenValiditySeconds;

	/** 客户端状态，控制客户端是否启用 */
	@Schema(description = "状态")
	private Status status;

	/** 令牌配置项，存储OAuth2.0令牌相关的自定义配置，序列化时忽略 */
	@JsonIgnore
	@Schema(hidden = true)
	private Map<String,Object> tokenSettings;

}
