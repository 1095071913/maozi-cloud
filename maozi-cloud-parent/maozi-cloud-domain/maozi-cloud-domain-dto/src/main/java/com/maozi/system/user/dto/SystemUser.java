package com.maozi.system.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 系统用户数据传输对象
 * <p>
 * 用于跨服务 RPC 调用时传输系统用户的基本信息。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemUser implements Serializable {

    /** 序列化标识 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 用户标识 */
	@Schema(description = "标识")
	private Long id;

    /** 用户账号 */
	@Schema(description = "账号")
	private String username;

    /** 用户名称 */
	@Schema(description = "名称")
	private String name;

    /** 用户头像 */
	@Schema(description = "头像")
	private String icon;

}
