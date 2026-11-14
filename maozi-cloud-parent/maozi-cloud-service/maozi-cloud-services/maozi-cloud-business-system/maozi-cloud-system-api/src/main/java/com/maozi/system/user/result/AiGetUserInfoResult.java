package com.maozi.system.user.result;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.maozi.base.plugin.mapping.QueryMapping;
import com.maozi.base.result.DropDownResult;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * AI 工具查询用户信息结果
 * <p>
 * 供 AI 工具调用查询用户信息时使用的返回结果封装，
 * 包含用户ID、所属客户端ID、用户名称、客户端信息以及该用户拥有的权限列表。
 * </p>
 *
 * @author pengjinlong
 * @since 2026/8/29 07:22
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiGetUserInfoResult implements Serializable {
    /** 序列化标识 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 用户ID，JSON 序列化时字段名为 userId */
    @Schema(description = "用户ID")
    @JsonProperty("userId")
    private Long id;

    /** 所属客户端ID */
    @Schema(description = "客户端ID")
    private Long clientId;

    /** 用户名称 */
    @Schema(description = "用户名称")
    private String name;

    /** 所属客户端信息，通过远程客户端服务查询自动填充 */
    @Schema(description = "客户端")
    @QueryMapping(isService = true, serviceName = "rpcClientService", relationField = "clientId")
    private DropDownResult client;

    /** 用户拥有的权限列表，通过用户服务查询自动填充 */
    @Schema(description = "权限列表")
    @QueryMapping(isService = true, serviceName = "userServiceImpl",
            functionName = "getPermissionsByUserId", relationField = "id")
    private List<String> permissions;

}
