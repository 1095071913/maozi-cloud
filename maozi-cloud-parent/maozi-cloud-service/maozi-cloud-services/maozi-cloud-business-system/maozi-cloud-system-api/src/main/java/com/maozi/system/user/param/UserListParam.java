package com.maozi.system.user.param;

import com.maozi.base.param.plugin.OrderParam;
import com.maozi.base.plugin.context.QueryEnvironmentContext;
import com.maozi.base.plugin.query.QueryBaseType;
import com.maozi.base.plugin.query.QueryPlugin;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 用户列表查询参数
 * <p>
 * 用于用户列表分页查询时的请求参数封装。
 * 支持按用户名称模糊查询，并实现了排序参数接口以支持自定义排序。
 * 默认按创建时间倒序排列。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
@Data
public class UserListParam implements OrderParam, Serializable {

	/** 序列化标识 */
	@Serial
	private static final long serialVersionUID = 1L;

	/** 用户名称（模糊查询） */
	@Schema(description = "名称")
	@QueryPlugin(value = QueryBaseType.LIKE)
	private String name;

	/** 排序字段映射，key为字段名，value为排序方向（true 降序、false 升序、null 不排序） */
	private Map<String, Boolean> orderFieldMap;

	/** 升序排序字段映射 */
	private Map<String, List<String>> orderAscFieldsMap;

	/** 降序排序字段映射 */
	private Map<String, List<String>> orderDescFieldsMap;

	/** 主排序字段映射，默认按创建时间倒序排列 */
	private final Map<String, Map<String, Boolean>> orderMainFieldsMap = Map.of(
    	QueryEnvironmentContext.DEFAULT_ORDER_KEY, Map.of(
			CREATE_TIME_KEY, true
		)
	);

}