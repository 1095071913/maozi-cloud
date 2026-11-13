package com.maozi.ai.ai.function.call;

import com.maozi.common.context.CurrentUserInfoContext;
import com.maozi.common.dto.CurrentUserInfo;
import com.maozi.common.result.AbstractBaseResult;
import com.maozi.service.api.annotation.RemoteResource;
import com.maozi.system.user.api.rpc.RpcUserService;
import com.maozi.system.user.result.AiGetUserInfoResult;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

/**
 * 系统用户信息 AI 工具调用
 * <p>
 * 供 AI 对话通过 Spring AI 工具调用（Function Call）方式查询
 * 当前登录用户的用户、客户端、权限等信息。
 * </p>
 *
 * @author pengjinlong
 * @since 2026/8/29 07:03
 */
@Component
public class SystemFunctionCall {

    /** 系统用户 RPC 服务，用于查询用户相关信息 */
    @RemoteResource
    private RpcUserService rpcUserService;

    /**
     * 查询当前登录用户的用户、客户端、权限等信息
     *
     * @return 返回当前用户的用户 ID、所属客户端、用户名称以及权限列表等信息
     */
    @Tool(description = "查询用户、客户端、权限等信息")
    public AbstractBaseResult<AiGetUserInfoResult> getUserInfo() {
        Long userId = CurrentUserInfoContext.CONTEXT.get(CurrentUserInfo::getUserId);
        return rpcUserService.rpcAiGetUserInfo(userId);
    }

}
