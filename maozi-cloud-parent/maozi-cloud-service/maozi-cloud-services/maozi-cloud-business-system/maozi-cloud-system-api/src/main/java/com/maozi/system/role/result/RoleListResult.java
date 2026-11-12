package com.maozi.system.role.result;

import com.maozi.base.enums.Status;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 角色列表视图对象
 * <p>
 * 用于角色列表展示时的视图对象，包含角色的基本信息，
 * 如ID、名称、描述、创建时间和状态等。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Data
public class RoleListResult implements Serializable {
    /** 序列化标识 */
	@Serial
    private static final long serialVersionUID = 1L;

	/** 角色ID */
	@Schema(description = "标识")
	private Long id;

	/** 角色名称 */
	@Schema(description = "名称")
	private String name;

	/** 角色描述 */
	@Schema(description = "描述")
	private String description;

	/** 角色创建时间 */
	@Schema(description = "创建时间")
	private LocalDateTime createTime;

	/** 角色状态（启用/禁用） */
	@Schema(description = "状态")
	private Status status;

}
