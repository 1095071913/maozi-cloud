package com.maozi.system.config.api.impl.rest;

import cn.hutool.extra.cglib.CglibUtil;
import com.maozi.common.ResultUtil;
import com.maozi.common.result.AbstractBaseResult;
import com.maozi.service.api.annotation.RestService;
import com.maozi.system.config.api.impl.SystemServiceImpl;
import com.maozi.system.config.api.rest.RestSystemService;
import com.maozi.system.config.result.SystemPropertiesResult;

/**
 * 系统 REST 服务实现类
 * <p>
 * 继承自 SystemServiceImpl，实现 RestSystemService 接口，
 * 提供 RESTful 风格的系统配置信息查询接口，
 * 将系统属性配置转换为 SystemPropertiesResult 结果对象返回给前端使用。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
@RestService
public class RestSystemServiceImpl extends SystemServiceImpl implements RestSystemService {

	/**
	 * 获取系统配置属性信息
	 * <p>
	 * 将 SystemProperties 配置对象拷贝为 SystemPropertiesResult 结果对象，
	 * 供前端展示和使用。
	 * </p>
	 *
	 * @return 返回包含系统配置属性信息的统一响应结果
	 */
	@Override
	public AbstractBaseResult<SystemPropertiesResult> restGet() {
		return ResultUtil.success(CglibUtil.copy(systemProperties, SystemPropertiesResult.class));
	}

}
