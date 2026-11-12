package com.maozi.system.config.api.impl.rpc;

import com.maozi.base.result.DropDownResult;
import com.maozi.common.ResultUtil;
import com.maozi.common.result.AbstractBaseResult;
import com.maozi.service.api.annotation.RemoteService;
import com.maozi.system.config.api.impl.RegionServiceImpl;
import com.maozi.system.config.api.rpc.RpcRegionService;

/**
 * 地区 RPC 服务实现类
 * <p>
 * 继承自 RegionServiceImpl，实现 RpcRegionService 接口，
 * 基于 Apache Dubbo 协议提供远程过程调用（RPC）服务，
 * 供其他微服务通过 RPC 方式调用地区相关的接口。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@RemoteService
public class RpcRegionServiceImpl extends RegionServiceImpl implements RpcRegionService {

	/**
	 * 根据ID远程获取地区下拉信息
	 * <p>
	 * 通过地区ID查询对应的地区信息，并转换为下拉选项格式返回（仅包含地区 ID 与名称）；
	 * 地区ID为空或地区不存在时抛出业务异常。
	 * </p>
	 *
	 * @param id 地区ID，用于查询对应的地区记录
	 * @return 返回包含地区下拉信息的统一响应结果
	 */
	@Override
	public AbstractBaseResult<DropDownResult> rpcGet(Long id) {
		// 根据ID查询地区信息并转换为下拉选项格式
		return ResultUtil.success(dropDown(id));
	}

}
