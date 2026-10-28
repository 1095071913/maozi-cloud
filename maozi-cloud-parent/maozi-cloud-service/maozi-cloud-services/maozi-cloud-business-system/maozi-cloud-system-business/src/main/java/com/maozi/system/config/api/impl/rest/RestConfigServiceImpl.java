package com.maozi.system.config.api.impl.rest;

import com.maozi.base.enums.Status;
import com.maozi.base.param.PageParam;
import com.maozi.base.param.RequestParam;
import com.maozi.base.result.PageResult;
import com.maozi.common.ResultUtil;
import com.maozi.common.result.AbstractBaseResult;
import com.maozi.service.api.annotation.RestService;
import com.maozi.system.config.api.impl.ConfigServiceImpl;
import com.maozi.system.config.api.rest.RestConfigService;
import com.maozi.system.config.param.ConfigListParam;
import com.maozi.system.config.param.ConfigSaveUpdateParam;
import com.maozi.system.config.result.ConfigDropDownResult;
import com.maozi.system.config.result.ConfigInfoResult;
import com.maozi.system.config.result.ConfigListResult;

import java.util.List;

/**
 * 全局配置 REST 服务实现类
 * <p>
 * 继承自 ConfigServiceImpl，实现 RestConfigService 接口，
 * 提供全局配置模块的 RESTful API 接口实现，包括配置的
 * 分页查询、新增、详情查询、删除、更新、状态变更、
 * 下拉列表以及按名称获取配置等功能。
 * </p>
 *
 * @author maozi
 */
@RestService
public class RestConfigServiceImpl extends ConfigServiceImpl implements RestConfigService {

	/**
	 * 分页查询全局配置列表
	 *
	 * @param pageParam 分页查询参数，包含分页信息和配置列表查询条件（类型、名称、别名）
	 * @return 分页查询结果，包含配置列表数据
	 */
	@Override
	public AbstractBaseResult<PageResult<ConfigListResult>> restList(PageParam<ConfigListParam> pageParam) {
		return ResultUtil.success(listRelation(pageParam, ConfigListResult.class));
	}

	/**
	 * 新增全局配置
	 *
	 * @param param 配置新增参数，包含配置名称、别名、类型、配置值与排序
	 * @return 新增配置的 ID
	 */
	@Override
	public AbstractBaseResult<Long> restSave(ConfigSaveUpdateParam param) {
		return ResultUtil.success(restSaveUpdate(null,param));
	}

	/**
	 * 获取配置下拉列表
	 * <p>
	 * 根据配置类型（必传）返回该类型下所有启用状态的配置选项，
	 * 每项包含配置 ID、名称、别名、配置值与排序。
	 * </p>
	 *
	 * @param type 配置类型（必传），用于筛选指定类型的配置选项
	 * @return 包含下拉列表数据的统一响应结果
	 */
	@Override
	public AbstractBaseResult<List<ConfigDropDownResult>> dropDownListResult(String type) {
		return ResultUtil.success(dropDownList(type));
	}

	/**
	 * 根据配置名称获取配置
	 * <p>
	 * 配置名称为全局唯一键，配置名称为空时抛出业务异常，配置不存在时返回 null。
	 * </p>
	 *
	 * @param name 配置名称
	 * @return 该配置名称对应的配置信息，包含配置 ID、名称、别名与配置值；配置不存在时 data 为 null
	 */
	@Override
	public AbstractBaseResult<ConfigDropDownResult> restDropDown(String name) {
		return ResultUtil.success(getConfigByName(name));
	}

	/**
	 * 查询配置详情
	 *
	 * @param id 配置 ID
	 * @return 配置详细信息
	 */
	@Override
	public AbstractBaseResult<ConfigInfoResult> restGet(Long id) {
		return ResultUtil.success(getByIdThrowErrorRelation(id, ConfigInfoResult.class));
	}

	/**
	 * 删除配置
	 *
	 * @param id 需要删除的配置 ID
	 * @return 操作结果
	 */
	@Override
	public AbstractBaseResult<Void> restRemove(Long id) {
		return removeByIdResult(id);
	}

	/**
	 * 更新配置信息
	 *
	 * @param id    需要更新的配置 ID
	 * @param param 配置更新参数，包含需要修改的名称、别名、类型、配置值与排序
	 * @return 操作结果
	 */
	@Override
	public AbstractBaseResult<Void> restUpdate(Long id, ConfigSaveUpdateParam param) {
		restSaveUpdate(id,param);
		return ResultUtil.success();
	}

	/**
	 * 更新配置状态（启用/禁用）
	 *
	 * @param id    需要更新状态的配置 ID
	 * @param param 状态参数，包含需要更新的目标状态值（启用/禁用）
	 * @return 无返回数据，仅返回操作结果状态
	 */
	@Override
	public AbstractBaseResult<Void> restUpdateStatusResult(Long id, RequestParam<Status> param) {
		return updateStatusResult(id, param);
	}

}
