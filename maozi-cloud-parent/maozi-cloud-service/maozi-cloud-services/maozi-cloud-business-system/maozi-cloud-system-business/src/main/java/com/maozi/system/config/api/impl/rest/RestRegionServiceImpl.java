package com.maozi.system.config.api.impl.rest;

import com.maozi.base.result.DropDownResult;
import com.maozi.common.ResultUtil;
import com.maozi.common.result.AbstractBaseResult;
import com.maozi.service.api.annotation.RestService;
import com.maozi.service.api.impl.result.BaseServiceResultImpl;
import com.maozi.system.config.api.impl.RegionServiceImpl;
import com.maozi.system.config.api.rest.RestRegionService;

import java.util.List;

/**
 * 地区 REST 服务实现类
 * <p>
 * 委托注入的 RegionServiceImpl 完成业务处理，提供地区数据的 RESTful 查询接口，
 * 主要用于前端下拉列表数据获取。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@RestService
public class RestRegionServiceImpl extends BaseServiceResultImpl<RegionServiceImpl, Void> implements RestRegionService {

	/**
	 * 根据父级ID查询子地区下拉列表
	 *
	 * @param parentId 父级地区ID
	 * @return 子地区下拉列表，每项包含地区ID和名称
	 */
	@Override
	public AbstractBaseResult<List<DropDownResult>> listResult(Long parentId) {
		return ResultUtil.success(service.commonList(parentId));
	}

}
