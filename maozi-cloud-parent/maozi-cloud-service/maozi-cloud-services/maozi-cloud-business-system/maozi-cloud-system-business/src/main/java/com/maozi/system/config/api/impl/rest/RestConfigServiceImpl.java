package com.maozi.system.config.api.impl.rest;

import com.maozi.base.param.PageParam;
import com.maozi.base.result.PageResult;
import com.maozi.common.ResultUtil;
import com.maozi.common.result.AbstractBaseResult;
import com.maozi.common.result.error.exception.BusinessResultException;
import com.maozi.service.api.annotation.RestService;
import com.maozi.service.api.impl.result.BaseServiceResultImpl;
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
 * 委托注入的 ConfigServiceImpl 完成业务处理，提供配置的分页查询、新增、更新、
 * 详情查询、下拉列表及按名称获取配置等 RESTful 接口。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@RestService
public class RestConfigServiceImpl extends BaseServiceResultImpl<ConfigServiceImpl, Void> implements RestConfigService {

	/**
	 * 分页查询全局配置列表
	 *
	 * @param pageParam 分页查询参数，包含分页信息与查询条件
	 * @return 配置列表分页结果
	 */
	@Override
	public AbstractBaseResult<PageResult<ConfigListResult>> listResult(PageParam<ConfigListParam> pageParam) {
		return ResultUtil.success(service.listRelation(pageParam, ConfigListResult.class));
	}

	/**
	 * 新增全局配置
	 *
	 * @param param 配置新增参数
	 * @return 新增配置的 ID
	 */
	@Override
	public AbstractBaseResult<Long> saveResult(ConfigSaveUpdateParam param) {
		return ResultUtil.success(service.commonSaveUpdate(null, param));
	}

	/**
	 * 获取指定类型的配置下拉列表
	 * <p>
	 * 仅返回启用状态的配置选项。
	 * </p>
	 *
	 * @param type 配置类型
	 * @return 配置下拉列表数据
	 */
	@Override
	public AbstractBaseResult<List<ConfigDropDownResult>> dropDownListResult(String type) {
		return ResultUtil.success(service.commonDropDownList(type));
	}

	/**
	 * 根据配置名称获取配置
	 * <p>
	 * 配置名称为全局唯一键，为空时抛出业务异常。
	 * </p>
	 *
	 * @param name 配置名称
	 * @return 配置信息，包含配置 ID、名称、别名与配置值；配置不存在时 data 为 null
	 */
	@Override
	public AbstractBaseResult<ConfigDropDownResult> dropDownResult(String name) {
		return ResultUtil.success(service.commonGetConfigByName(name));
	}

	/**
	 * 查询配置详情
	 *
	 * @param id 配置 ID
	 * @return 配置详细信息
	 * @throws BusinessResultException 当配置不存在时抛出异常
	 */
	@Override
	public AbstractBaseResult<ConfigInfoResult> getResult(Long id) {
		return ResultUtil.success(service.getByIdThrowErrorRelation(id, ConfigInfoResult.class));
	}

	/**
	 * 更新配置信息
	 *
	 * @param id    需要更新的配置 ID
	 * @param param 配置更新参数
	 * @return 操作结果
	 */
	@Override
	public AbstractBaseResult<Void> updateResult(Long id, ConfigSaveUpdateParam param) {
		service.saveUpdate(id, param);
		return ResultUtil.success();
	}

}
