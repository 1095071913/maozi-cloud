package com.maozi.system.config.api.rpc;

import com.maozi.base.result.DropDownResult;
import com.maozi.common.result.AbstractBaseResult;

/**
 * 地区 RPC 服务接口
 * <p>
 * 定义地区信息相关的 RPC 远程调用方法。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
public interface RpcRegionService {

    /**
     * 根据 ID 获取地区下拉信息
     *
     * @param id 地区 ID
     * @return 地区下拉结果
     */
	AbstractBaseResult<DropDownResult> rpcGet(Long id);

}
