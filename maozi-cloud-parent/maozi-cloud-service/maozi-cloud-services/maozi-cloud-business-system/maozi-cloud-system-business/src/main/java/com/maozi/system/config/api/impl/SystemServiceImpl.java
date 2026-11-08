package com.maozi.system.config.api.impl;

import com.maozi.system.config.api.SystemService;
import com.maozi.system.config.properties.SystemProperties;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/**
 * 系统服务实现类
 * <p>
 * 实现 SystemService 接口，提供系统级别的配置属性访问功能。
 * 通过注入 SystemProperties 获取系统相关的配置信息。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
@Service
public class SystemServiceImpl implements SystemService {

	/** 系统配置属性，包含项目名称、公司名称、图标等系统级配置信息 */
	@Resource
	protected SystemProperties systemProperties;

}
