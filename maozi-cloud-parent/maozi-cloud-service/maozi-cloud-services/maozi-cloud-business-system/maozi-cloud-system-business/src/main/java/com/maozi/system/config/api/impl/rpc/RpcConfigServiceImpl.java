package com.maozi.system.config.api.impl.rpc;

import com.maozi.common.ObjectUtil;
import com.maozi.common.ResultUtil;
import com.maozi.common.result.AbstractBaseResult;
import com.maozi.service.api.annotation.RemoteService;
import com.maozi.service.api.impl.result.BaseServiceResultImpl;
import com.maozi.system.config.api.impl.ConfigServiceImpl;
import com.maozi.system.config.api.rpc.RpcConfigService;
import com.maozi.system.config.result.ConfigDropDownResult;

/**
 * 全局配置 RPC 服务实现类
 * <p>
 * 委托注入的 ConfigServiceImpl 完成业务处理，基于 Dubbo RPC 供其他微服务获取系统全局配置。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@RemoteService
public class RpcConfigServiceImpl extends BaseServiceResultImpl<ConfigServiceImpl, Void> implements RpcConfigService {

	/**
	 * 根据配置键名远程获取配置值
	 * <p>
	 * 配置键名即配置名称，全局唯一；为空时抛出业务异常。
	 * </p>
	 *
	 * @param key 配置键名，即配置名称
	 * @return 配置值；配置不存在时 data 为 null
	 */
	@Override
	public AbstractBaseResult<String> getResult(String key) {
		ConfigDropDownResult configResult = service.commonGetConfigByName(key);
		return ResultUtil.success(ObjectUtil.isNotNullEmpty(configResult) ? configResult.getValue() : null);
	}

}
