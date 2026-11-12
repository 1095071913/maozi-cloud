package com.maozi.oauth.client.api.impl.rpc;

import com.maozi.common.result.AbstractBaseResult;
import com.maozi.oauth.client.api.impl.ClientServiceImpl;
import com.maozi.oauth.client.api.rpc.RpcClientService;
import com.maozi.oauth.client.dto.ClientDto;
import com.maozi.oauth.client.param.ClientSaveUpdateParam;
import com.maozi.service.api.annotation.RemoteService;
import com.maozi.service.api.impl.result.BaseServiceResultImpl;

/**
 * OAuth2客户端RPC服务实现类
 * <p>
 * 继承 ClientServiceImpl 并实现 RpcClientService 接口，通过 @RemoteService 注解
 * 将客户端服务能力暴露为微服务间RPC调用；除 rpcAiSave（供AI智能体调用）外，
 * 其余方法均直接继承自 ClientServiceImpl。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@RemoteService
public class RpcClientServiceImpl extends BaseServiceResultImpl<ClientServiceImpl, ClientDto> implements RpcClientService {

    /**
     * 新增客户端并返回保存后的客户端信息
     * <p>
     * 供AI智能体等远程调用方使用：先以新增方式保存客户端，
     * 再根据保存后的主键ID查询并封装为客户端DTO返回。
     * </p>
     *
     * @param param 客户端保存参数
     * @return 包含新增客户端信息的统一响应结果
     */
    @Override
    public AbstractBaseResult<ClientDto> rpcAiSave(ClientSaveUpdateParam param) {
        Long clientId = service.restSaveUpdate(null, param);
        return getByIdResult(clientId);
    }

}
