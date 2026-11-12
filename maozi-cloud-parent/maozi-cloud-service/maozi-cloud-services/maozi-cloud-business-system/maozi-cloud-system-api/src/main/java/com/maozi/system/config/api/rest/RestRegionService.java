package com.maozi.system.config.api.rest;

import com.maozi.base.result.DropDownResult;
import com.maozi.common.result.AbstractBaseResult;
import com.maozi.service.annotation.Get;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * 地区服务 REST 接口
 * <p>
 * 提供地区数据的 RESTful API 接口定义，主要用于查询地区下拉列表信息。
 * 支持根据父级地区 ID 获取对应的子地区列表。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Tag(name = "地区模块")
public interface RestRegionService {

	/** 基础路径常量，地区模块的统一请求路径前缀 */
	String PATH = "/config";

	/**
	 * 根据父级地区 ID 获取地区下拉列表
	 *
	 * @param parentId 父级地区 ID，用于查询该地区下的所有子级地区
	 * @return 返回地区下拉列表结果，仅包含地区 ID 和名称两项基本信息
	 */
	@Get(value = PATH + "/region/{parentId}/list",description = "列表")
	AbstractBaseResult<List<DropDownResult>> restList(@PathVariable("parentId") Long parentId);

}