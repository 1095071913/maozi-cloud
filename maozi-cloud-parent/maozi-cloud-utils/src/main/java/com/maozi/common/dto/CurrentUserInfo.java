package com.maozi.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 当前登录用户信息
 * <p>
 * 通过 {@code CurrentUserInfoContext} 在链路中传递的最小化用户身份信息，
 * 仅保留用户 ID、客户端 ID、用户名、权限列表四个核心字段，避免在日志、上下文中暴露完整 User 实体。
 * </p>
 *
 * @author pengjinlong
 * @since 2026/6/16 16:51
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CurrentUserInfo implements Serializable {

    /** 序列化标识 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 用户ID */
    private Long userId;

    /** 客户端 ID（OAuth2 Registered Client 主键） */
    private Long clientId;

    /** 用户名 */
    private String username;

    /** 权限列表 */
    private List<String> permissions;

}
