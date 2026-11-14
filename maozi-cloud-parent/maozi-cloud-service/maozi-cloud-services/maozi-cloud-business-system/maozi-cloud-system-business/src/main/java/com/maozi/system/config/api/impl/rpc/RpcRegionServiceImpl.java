package com.maozi.system.config.api.impl.rpc;

import com.maozi.base.result.DropDownResult;
import com.maozi.common.ResultUtil;
import com.maozi.common.result.AbstractBaseResult;
import com.maozi.service.api.annotation.RemoteService;
import com.maozi.service.api.impl.result.BaseServiceResultImpl;
import com.maozi.system.config.api.impl.RegionServiceImpl;
import com.maozi.system.config.api.rpc.RpcRegionService;

/**
 * 地区 RPC 服务实现类
 * <p>
 * 委托注入的 RegionServiceImpl 完成业务处理，基于 Dubbo RPC 供其他微服务调用地区相关接口。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@RemoteService
public class RpcRegionServiceImpl extends BaseServiceResultImpl<RegionServiceImpl, Void> implements RpcRegionService {

	/**
	 * 根据ID远程获取地区下拉信息
	 * <p>
	 * 地区ID为空或地区不存在时抛出业务异常。
	 * </p>
	 *
	 * @param id 地区ID
	 * @return 地区下拉信息，仅包含地区 ID 与名称
	 */
	@Override
	public AbstractBaseResult<DropDownResult> getResult(Long id) {
		return ResultUtil.success(service.dropDown(id));
	}

}
