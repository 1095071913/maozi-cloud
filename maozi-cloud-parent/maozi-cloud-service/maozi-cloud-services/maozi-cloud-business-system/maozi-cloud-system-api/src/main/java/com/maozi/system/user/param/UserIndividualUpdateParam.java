package com.maozi.system.user.param;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户个人信息更新参数
 * <p>
 * 用于当前登录用户修改个人信息的请求参数封装，
 * 包含姓名、头像以及修改密码所需的旧密码和新密码。
 * </p>
 *
 * @author pengjinlong
 * @date 2026/8/13
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserIndividualUpdateParam implements Serializable {
    /** 序列化标识 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 用户姓名 */
    @Schema(description = "名称")
    private String name;

    /** 用户头像图标 */
    @Schema(description = "头像")
    private String icon;

    /** 修改密码时的旧密码 */
    @Schema(description = "旧密码")
    private String password;

    /** 修改密码时的新密码 */
    @Schema(description = "新密码")
    private String newPassword;

}
