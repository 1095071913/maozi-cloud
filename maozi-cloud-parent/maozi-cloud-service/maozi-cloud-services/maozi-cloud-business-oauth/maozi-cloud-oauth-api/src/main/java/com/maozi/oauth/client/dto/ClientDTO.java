package com.maozi.oauth.client.dto;

import com.maozi.base.enums.Status;
import com.maozi.oauth.client.enums.AuthType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Set;

/**
 * 客户端信息传输对象
 * <p>
 * 用于RPC调用等场景传输OAuth2客户端的配置信息，
 * 包括客户端ID、密钥、名称、授权模式、令牌有效期、备注及状态等。
 * </p>
 *
 * @author pengjinlong
 * @since 2026/8/29 21:30
 */
@Data
public class ClientDTO implements Serializable {
    /** 序列化标识 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 客户端唯一标识 */
    @Schema(description = "客户端ID")
    private String clientId;

    /** 客户端密钥，用于身份验证 */
    @Schema(description = "客户端密钥")
    private String clientSecret;

    /** 客户端名称 */
    @Schema(description = "名称")
    private String name;

    /** 授权模式集合，客户端支持的 OAuth2.0 授权类型 */
    @Schema(description = "授权模式", ref = "IntegerArrayList")
    private Set<AuthType> authorizationGrantTypes;

    /** 授权令牌（AccessToken）有效期，单位为秒 */
    @Schema(description = "授权令牌有效期 秒 默认2小时")
    private Long accessTokenValiditySeconds;

    /** 刷新令牌（RefreshToken）有效期，单位为秒 */
    @Schema(description = "刷新令牌有效期 秒 默认7天")
    private Long refreshTokenValiditySeconds;

    /** 备注 */
    @Schema(description = "备注")
    private String remark;

    /** 客户端状态（ENABLE 启用 / DISABLE 禁用） */
    @Schema(description = "状态")
    private Status status;

}
