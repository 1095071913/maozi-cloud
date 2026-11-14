package com.maozi.system.config.api.rest;

import com.maozi.common.result.AbstractBaseResult;
import com.maozi.service.annotation.Get;
import com.maozi.system.config.result.SystemPropertiesResult;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 系统配置服务 REST 接口
 *
 * @author maozi
 * @date 2026/11/08
 */
@Tag(name = "系统模块")
public interface RestSystemService {

	/** 基础路径常量，系统配置模块的统一请求路径前缀 */
	String PATH = "/config";

	/**
	 * 获取系统属性配置详情
	 *
	 * @return 返回系统属性配置信息，包含项目名称、公司名称、图标、运行环境、项目描述与版权信息
	 */
	@Get(value = PATH + "/system/get",description = "详情")
	AbstractBaseResult<SystemPropertiesResult> getResult();

}
