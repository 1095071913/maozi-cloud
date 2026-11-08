package com.maozi.oauth.token.param;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.io.Serializable;

/**
 * 客户端用户参数
 * <p>
 * 以客户端 ID 和用户名标识某客户端下的用户主体，用于按用户主体批量注销令牌等场景。
 * </p>
 *
 * @author maozi
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
public class ClientUserParam implements Serializable {

    /** 序列化标识 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 客户端 ID */
    @Schema(description = "客户端ID")
    private Long clientId;

    /** 用户名 */
    @Schema(description = "用户名")
    private String username;

}
