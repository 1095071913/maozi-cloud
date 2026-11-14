package com.maozi.system.config.api.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.maozi.base.result.DropDownResult;
import com.maozi.service.api.impl.BaseServiceImpl;
import com.maozi.system.config.api.RegionService;
import com.maozi.system.config.domain.RegionDO;
import com.maozi.system.config.mapper.RegionMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 地区服务实现类
 * <p>
 * 继承 BaseServiceImpl，提供地区的基础业务操作实现。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Service
public class RegionServiceImpl extends BaseServiceImpl<RegionMapper, RegionDO> implements RegionService {

	/** 资源名称，用于日志和异常信息中标识当前操作的资源类型 */
	private static final String RESOURCE_NAME = "地区";

	/**
	 * 获取资源名称
	 */
	@Override
	public String getResourceName() {
		return RESOURCE_NAME;
	}

	/**
	 * 根据父级 ID 查询子地区下拉列表
	 *
	 * @param parentId 父级地区 ID
	 * @return 子地区下拉列表，每项包含地区 ID 和名称
	 */
	public List<DropDownResult> commonList(Long parentId) {

		LambdaQueryWrapper<RegionDO> wrapper = Wrappers.lambdaQuery();

		// 只查询ID和名称字段
		wrapper.select(RegionDO::getId,RegionDO::getName);
		wrapper.eq(RegionDO::getParentId,parentId);

		return super.list(wrapper,DropDownResult::new);

	}

}
