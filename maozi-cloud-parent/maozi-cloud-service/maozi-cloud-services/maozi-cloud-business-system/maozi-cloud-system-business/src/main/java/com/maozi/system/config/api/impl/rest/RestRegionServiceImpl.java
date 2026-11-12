package com.maozi.system.config.api.impl.rest;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.maozi.base.result.DropDownResult;
import com.maozi.common.ResultUtil;
import com.maozi.common.result.AbstractBaseResult;
import com.maozi.service.api.annotation.RestService;
import com.maozi.service.api.impl.result.BaseServiceResultImpl;
import com.maozi.system.config.api.impl.RegionServiceImpl;
import com.maozi.system.config.api.rest.RestRegionService;
import com.maozi.system.config.domain.RegionDo;

import java.util.List;

/**
 * 地区 REST 服务实现类
 * <p>
 * 继承自 RegionServiceImpl，实现 RestRegionService 接口，
 * 提供 RESTful 风格的地区查询接口，主要用于前端下拉列表数据获取。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@RestService
public class RestRegionServiceImpl extends BaseServiceResultImpl<RegionServiceImpl, Void> implements RestRegionService {

	/**
	 * 根据父级ID查询子地区下拉列表
	 * <p>
	 * 根据指定的父级地区ID，查询其下所有子地区信息，
	 * 并将结果转换为下拉列表（DropDownResult）格式返回。
	 * </p>
	 *
	 * @param parentId 父级地区ID，用于筛选子地区数据
	 * @return 返回包含下拉列表数据的统一响应结果，列表中每个元素包含地区的ID和名称
	 */
	@Override
	public AbstractBaseResult<List<DropDownResult>> restList(Long parentId) {

		LambdaQueryWrapper<RegionDo> wrapper = Wrappers.lambdaQuery();

		// 只查询ID和名称字段
		wrapper.select(RegionDo::getId,RegionDo::getName);
		// 根据父级ID筛选子地区
		wrapper.eq(RegionDo::getParentId,parentId);

		// 执行查询并将结果转换为下拉列表格式返回
		return ResultUtil.success(service.list(wrapper,DropDownResult::new));

	}

}
