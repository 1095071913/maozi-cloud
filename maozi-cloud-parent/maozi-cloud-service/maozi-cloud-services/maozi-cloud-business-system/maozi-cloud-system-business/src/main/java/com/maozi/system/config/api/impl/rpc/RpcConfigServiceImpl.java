package com.maozi.system.config.api.impl.rpc;

import com.maozi.common.ObjectUtil;
import com.maozi.common.ResultUtil;
import com.maozi.common.result.AbstractBaseResult;
import com.maozi.service.api.annotation.RemoteService;
import com.maozi.system.config.api.impl.ConfigServiceImpl;
import com.maozi.system.config.api.rpc.RpcConfigService;
import com.maozi.system.config.result.ConfigDropDownResult;

/**
 * 全局配置 RPC 服务实现类
 * <p>
 * 继承自 ConfigServiceImpl，实现 RpcConfigService 接口，
 * 基于 Apache Dubbo 协议提供远程过程调用（RPC）服务，
 * 供其他微服务通过 RPC 方式获取系统全局配置。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@RemoteService
public class RpcConfigServiceImpl extends ConfigServiceImpl implements RpcConfigService {

	/**
	 * 根据配置键名远程获取配置值
	 * <p>
	 * 配置键名即配置名称（name），全局唯一。
	 * 配置键名为空时抛出业务异常，配置不存在时返回 null（data 为空）。
	 * </p>
	 *
	 * @param key 配置键名（对应配置名称）
	 * @return 返回包含配置值的统一响应结果；配置不存在时 data 为 null
	 */
	@Override
	public AbstractBaseResult<String> rpcGet(String key) {
		ConfigDropDownResult configResult = getConfigByName(key);
		return ResultUtil.success(ObjectUtil.isNotNullEmpty(configResult) ? configResult.getValue() : null);
	}

}
