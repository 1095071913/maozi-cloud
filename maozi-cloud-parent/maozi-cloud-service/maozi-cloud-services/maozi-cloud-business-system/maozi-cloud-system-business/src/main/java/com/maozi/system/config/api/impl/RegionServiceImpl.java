package com.maozi.system.config.api.impl;

import com.maozi.service.api.impl.BaseServiceImpl;
import com.maozi.system.config.api.RegionService;
import com.maozi.system.config.domain.RegionDo;
import com.maozi.system.config.mapper.RegionMapper;
import org.springframework.stereotype.Service;

/**
 * 地区服务实现类
 * <p>
 * 继承自 BaseServiceImpl，提供地区（Region）相关的基础业务操作实现。
 * 基于 MyBatis-Plus 的 Mapper 层实现数据库访问操作。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Service
public class RegionServiceImpl extends BaseServiceImpl<RegionMapper,RegionDo,Void> implements RegionService {

	/** 资源名称，用于日志和异常信息中标识当前操作的资源类型 */
	private static final String RESOURCE_NAME = "地区";

	/**
	 * 获取资源名称
	 *
	 * @return 返回当前服务所管理的资源名称 "地区"
	 */
	@Override
	public String getResourceName() {
		return RESOURCE_NAME;
	}

}
