package com.maozi.system.config.param;

import com.maozi.base.param.plugin.OrderParam;
import com.maozi.base.plugin.context.QueryEnvironmentContext;
import com.maozi.base.plugin.query.QueryBaseType;
import com.maozi.base.plugin.query.QueryPlugin;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 全局配置列表查询参数
 * <p>
 * 用于全局配置分页查询时的请求参数封装。
 * 支持按配置类型精确查询、按配置名称与配置别名模糊查询，
 * 并实现了排序参数接口以支持自定义排序，默认按排序值升序、创建时间倒序排列。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
@Data
public class ConfigListParam implements OrderParam, Serializable {
	/** 序列化标识 */
	@Serial
	private static final long serialVersionUID = 1L;

	/** 配置名称（模糊查询） */
	@Schema(description = "名称")
	@QueryPlugin(value = QueryBaseType.LIKE)
	private String name;

	/** 配置别名（模糊查询） */
	@Schema(description = "别名")
	@QueryPlugin(value = QueryBaseType.LIKE)
	private String alias;

	/** 配置类型（精确查询） */
	@Schema(description = "类型")
	@NotEmpty(message = "类型不能为空")
	@QueryPlugin(value = QueryBaseType.EQ)
	private String type;

	/** 排序字段映射，key为字段名，value为排序方向（true 降序、false 升序、null 不排序） */
	private Map<String, Boolean> orderFieldMap;

	/** 升序排序字段映射，key为表别名，value为该表下需要升序排序的字段列表 */
	private Map<String, List<String>> orderAscFieldsMap;

	/** 降序排序字段映射，key为表别名，value为该表下需要降序排序的字段列表 */
	private Map<String, List<String>> orderDescFieldsMap;

	/** 主表可排序字段映射，key为表别名，value为该表下可排序字段及默认排序方向（true 降序、false 升序），默认按排序值升序、创建时间倒序排列 */
	private final Map<String, Map<String, Boolean>> orderMainFieldsMap = Map.of(
		QueryEnvironmentContext.DEFAULT_ORDER_KEY, Map.of(
			"sort",false,
				CREATE_TIME_KEY, true
		)
	);

}
