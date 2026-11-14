package com.maozi.oauth.client.api.impl.rest;

import com.maozi.base.param.PageParam;
import com.maozi.base.result.PageResult;
import com.maozi.common.ResultUtil;
import com.maozi.common.result.AbstractBaseResult;
import com.maozi.oauth.client.api.impl.ClientServiceImpl;
import com.maozi.oauth.client.api.rest.RestClientService;
import com.maozi.oauth.client.dto.ClientDTO;
import com.maozi.oauth.client.param.ClientListParam;
import com.maozi.oauth.client.param.ClientSaveUpdateParam;
import com.maozi.oauth.client.result.ClientInfoResult;
import com.maozi.oauth.client.result.ClientListResult;
import com.maozi.service.api.annotation.RestService;
import com.maozi.service.api.impl.result.BaseServiceResultImpl;


/**
 * OAuth2客户端REST接口实现类。
 * <p>
 * 委托注入的 ClientServiceImpl 完成业务处理，
 * 未覆写的方法沿用 BaseServiceResultImpl 的通用实现。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@RestService
public class RestClientServiceImpl extends BaseServiceResultImpl<ClientServiceImpl, ClientDTO> implements RestClientService {

	/**
	 * 分页查询客户端列表。
	 */
	@Override
	public AbstractBaseResult<PageResult<ClientListResult>> listResult(PageParam<ClientListParam> pageParam) {
		return ResultUtil.success(service.adminList(pageParam));
	}

	/**
	 * 新增客户端。
	 */
	@Override
	public AbstractBaseResult<Long> saveResult(ClientSaveUpdateParam param) {
		return ResultUtil.success(service.adminSaveUpdate(null, param));
	}

    /**
     * 根据主键ID获取客户端详细信息。
     */
    @Override
    public AbstractBaseResult<ClientInfoResult> getResult(Long id) {
        return ResultUtil.success(service.adminGet(id));
    }

    /**
     * 更新客户端信息。
     */
    @Override
    public AbstractBaseResult<Void> updateResult(Long id, ClientSaveUpdateParam param) {
		service.adminSaveUpdate(id, param);
        return ResultUtil.success();
    }


}