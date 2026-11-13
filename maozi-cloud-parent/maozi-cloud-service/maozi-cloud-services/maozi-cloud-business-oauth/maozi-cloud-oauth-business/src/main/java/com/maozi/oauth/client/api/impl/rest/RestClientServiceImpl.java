package com.maozi.oauth.client.api.impl.rest;

import com.maozi.base.enums.Status;
import com.maozi.base.param.PageParam;
import com.maozi.base.param.RequestParam;
import com.maozi.base.result.DropDownResult;
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

import java.util.List;


/**
 * OAuth2客户端REST接口实现类。
 * <p>
 * 继承 BaseServiceResultImpl 并实现 RestClientService 接口，
 * 委托注入的 ClientServiceImpl（service）完成业务处理，
 * 作为RESTful API的服务端实现，提供客户端的增删改查、
 * 状态更新、下拉列表等HTTP接口功能。
 * 使用 @RestService 注解标识为REST服务。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@RestService
public class RestClientServiceImpl extends BaseServiceResultImpl<ClientServiceImpl, ClientDTO> implements RestClientService {

	/**
	 * 分页查询客户端列表。
	 *
	 * @param pageParam 分页查询参数，包含查询条件和分页信息
	 * @return 包含客户端分页列表的统一响应结果
	 */
	@Override
	public AbstractBaseResult<PageResult<ClientListResult>> restList(PageParam<ClientListParam> pageParam) {
		return ResultUtil.success(service.superRestList(pageParam));
	}

	/**
	 * 新增客户端。
	 *
	 * @param param 客户端保存参数
	 * @return 包含新增客户端主键ID的统一响应结果
	 */
	@Override
	public AbstractBaseResult<Long> restSave(ClientSaveUpdateParam param) {
		return ResultUtil.success(service.saveUpdate(null, param));
	}

	/**
	 * 获取客户端下拉列表数据。
	 *
	 * @return 包含客户端下拉选项列表的统一响应结果
	 */
	@Override
	public AbstractBaseResult<List<DropDownResult>> restDropDownListResult() {
		return super.dropDownListResult();
	}

	/**
	 * 根据主键ID获取客户端详细信息。
	 *
	 * @param id 客户端主键ID
	 * @return 包含客户端详细信息的统一响应结果
	 */
    @Override
    public AbstractBaseResult<ClientInfoResult> restGet(Long id) {
        return ResultUtil.success(service.superRestGet(id));
    }

    /**
     * 更新客户端信息。
     *
     * @param id    客户端主键ID
     * @param param 客户端更新参数
     * @return 无数据的统一响应结果
     */
    @Override
    public AbstractBaseResult<Void> restUpdate(Long id, ClientSaveUpdateParam param) {
		service.saveUpdate(id, param);
        return ResultUtil.success();
    }

	/**
	 * 更新客户端状态（启用/禁用）。
	 *
	 * @param id    客户端主键ID
	 * @param param 状态更新参数，包含目标状态值
	 * @return 无数据的统一响应结果
	 */
	@Override
	public AbstractBaseResult<Void> restUpdateStatusResult(Long id, RequestParam<Status> param) {
		return super.updateStatusResult(id, param);
	}

	/**
     * 根据主键ID删除客户端（逻辑删除）。
     *
     * @param id 客户端主键ID
     * @return 无数据的统一响应结果
     */
    @Override
    public AbstractBaseResult<Void> restRemove(Long id) {
		service.removeById(id);
        return ResultUtil.success();
    }


}