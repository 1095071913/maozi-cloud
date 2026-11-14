package com.maozi.oauth.client.api.impl.rpc;

import com.maozi.common.result.AbstractBaseResult;
import com.maozi.oauth.client.api.impl.ClientServiceImpl;
import com.maozi.oauth.client.api.rpc.RpcClientService;
import com.maozi.oauth.client.dto.ClientDTO;
import com.maozi.oauth.client.param.ClientSaveUpdateParam;
import com.maozi.service.api.annotation.RemoteService;
import com.maozi.service.api.impl.result.BaseServiceResultImpl;

/**
 * OAuth2客户端RPC服务实现类
 * <p>
 * 除 aiSaveResult 在本类实现外，其余方法沿用 BaseServiceResultImpl 的通用实现，
 * 内部委托注入的 ClientServiceImpl 完成业务处理。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@RemoteService
public class RpcClientServiceImpl extends BaseServiceResultImpl<ClientServiceImpl, ClientDTO> implements RpcClientService {

    /**
     * 新增客户端并按保存后的主键ID查询返回客户端DTO
     *
     * @param param 客户端保存参数
     * @return 包含新增客户端信息的统一响应结果
     */
    @Override
    public AbstractBaseResult<ClientDTO> aiSaveResult(ClientSaveUpdateParam param) {
        Long clientId = service.adminSaveUpdate(null, param);
        return super.getByIdResult(clientId, getColumns(ClientDTO.class));
    }

}
