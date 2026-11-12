package com.maozi.system.user.result;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.maozi.base.enums.Status;
import com.maozi.base.plugin.mapping.QueryMapping;
import com.maozi.base.result.DropDownResult;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户列表视图对象
 * <p>
 * 用于用户列表展示时的视图对象，包含用户的基本信息，
 * 如ID、姓名、所属客户端、状态和创建时间等。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserListResult implements Serializable {
    /** 序列化标识 */
	@Serial
    private static final long serialVersionUID = 1L;

	/** 用户ID */
	@Schema(description = "标识")
	private Long id;

	/** 用户姓名 */
	@Schema(description = "名称")
	private String name;

	/** 所属客户端信息，通过远程服务查询获取 */
	@Schema(description = "客户端")
	@QueryMapping(relationField = "clientId",isService = true,serviceName = "rpcClientService")
	private DropDownResult client;

	/** 用户状态（启用/禁用） */
	@Schema(description = "状态")
	private Status status;

	/** 用户创建时间 */
	@Schema(description = "创建时间")
	private LocalDateTime createTime;

	/** 所属客户端ID，在JSON序列化时隐藏，用于关联查询 */
	@JsonIgnore
	@Schema(hidden = true)
	private Long clientId;

}
