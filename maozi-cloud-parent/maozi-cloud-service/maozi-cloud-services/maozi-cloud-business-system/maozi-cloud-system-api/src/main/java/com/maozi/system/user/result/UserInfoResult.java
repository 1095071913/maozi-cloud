package com.maozi.system.user.result;

import com.fasterxml.jackson.annotation.JsonFormat;
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
import java.util.List;

/**
 * 用户详情视图对象
 * <p>
 * 所属客户端与关联的角色ID列表通过 QueryMapping 注解自动查询填充。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoResult implements Serializable {
    /** 序列化标识 */
	@Serial
    private static final long serialVersionUID = 1L;

	/** 用户ID，在JSON序列化时隐藏 */
	@JsonIgnore
	@Schema(hidden = true)
	private Long id;

	/** 用户登录账号 */
	@Schema(description = "账号")
	private String username;

	/** 用户姓名 */
	@Schema(description = "名称")
	private String name;

	/** 所属客户端ID，在JSON序列化时隐藏 */
	@JsonIgnore
	@Schema(hidden = true)
	private Long clientId;

	/** 所属客户端信息，通过远程服务查询获取 */
	@Schema(description = "客户端")
	@QueryMapping(isService = true,serviceName = "rpcClientService",relationField = "clientId")
	private DropDownResult client;

	/** 用户头像图标 */
	@Schema(description = "图标")
	private String icon;

	/** 用户状态，以整数形式序列化 */
	@Schema(description = "状态")
	@JsonFormat(shape = JsonFormat.Shape.NUMBER_INT)
	private Status status;

	/** 用户关联的角色ID列表，通过用户角色服务查询自动填充 */
	@Schema(description = "角色列表",ref = "StringArrayList")
	@QueryMapping(isService = true, serviceName = "userRoleServiceImpl", functionName = "getRolesByUser", relationField = "id")
	private List<Long> roleIds;

}
