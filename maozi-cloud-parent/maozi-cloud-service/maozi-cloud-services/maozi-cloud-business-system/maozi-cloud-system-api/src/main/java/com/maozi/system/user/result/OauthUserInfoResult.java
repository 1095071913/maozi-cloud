package com.maozi.system.user.result;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * OAuth 认证用户信息结果
 * <p>
 * 用于用户模块向授权服务器返回认证所需的用户数据，
 * 包含用户ID、加密密码以及该用户拥有的权限标识列表。
 * </p>
 *
 * @author pengjinlong
 * @date 2026/8/13
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OauthUserInfoResult implements Serializable {
    /** 序列化标识 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 用户ID */
    @Schema(description = "用户ID")
    private Long userId;

    /** 用户加密后的登录密码 */
    @Schema(description = "用户密码")
    private String password;

    /** 用户拥有的权限标识列表 */
    @Schema(description = "权限列表")
    private List<String> authorities;

}
