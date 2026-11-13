package com.maozi.oauth.token.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * OAuth令牌传输对象
 * <p>
 * 用于封装OAuth2.0认证流程中返回的令牌信息，包括访问令牌、刷新令牌、
 * 令牌类型、有效期、授权范围以及OIDC的ID令牌等。
 * </p>
 *
 * @author pengjinlong
 * @since 2026/5/7 15:42
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OauthTokenDTO implements Serializable {
    /** 序列化标识 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 令牌类型，通常为 Bearer */
    @Schema(description = "鉴权方式")
    @JsonProperty("token_type")
    private String tokenType;

    /** 访问令牌（AccessToken），用于访问受保护资源 */
    @Schema(description = "认证令牌")
    @JsonProperty("access_token")
    private String accessToken;

    /** 刷新令牌（RefreshToken），用于获取新的访问令牌 */
    @Schema(description = "刷新令牌")
    @JsonProperty("refresh_token")
    private String refreshToken;

    /** 访问令牌有效期，单位秒 */
    @Schema(description = "令牌有效期")
    @JsonProperty("expires_in")
    private Long expiresIn;

    /** 刷新令牌有效期，单位秒 */
    @Schema(description = "刷新令牌有效期")
    @JsonProperty("refresh_token_expires_in")
    private Long refreshTokenExpiresIn;

    /** 授权范围，多个范围以空格分隔 */
    @Schema(description = "授权范围")
    private String scope;

    /** OIDC身份令牌（ID Token） */
    @Schema(description = "OIDC令牌")
    @JsonProperty("id_token")
    private String idToken;

}