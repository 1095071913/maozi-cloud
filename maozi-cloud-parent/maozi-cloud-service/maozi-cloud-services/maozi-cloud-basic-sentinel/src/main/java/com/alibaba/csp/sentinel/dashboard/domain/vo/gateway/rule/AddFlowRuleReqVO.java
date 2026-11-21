/*
 * Copyright 1999-2018 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.csp.sentinel.dashboard.domain.vo.gateway.rule;

/**
 * 新增网关流控规则请求 VO
 *
 * @author cdfive
 * @date 2026/11/22
 * @since 1.7.0
 */
public class AddFlowRuleReqVO extends BaseFlowRuleReqVO {

    /** 规则所属 Sentinel 客户端机器的 IP */
    private String ip;

    /** 规则所属 Sentinel 客户端机器的端口 */
    private Integer port;

    /** 规则限流的资源：路由 ID 或 API 名称，取决于 resourceMode */
    private String resource;

    /** 资源模式：0 表示路由 ID，1 表示自定义 API 名称（见 SentinelGatewayConstants） */
    private Integer resourceMode;

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public Integer getResourceMode() {
        return resourceMode;
    }

    public void setResourceMode(Integer resourceMode) {
        this.resourceMode = resourceMode;
    }

    @Override
    public String toString() {
        return "AddFlowRuleReqVO{"
                + "app='" + app + '\''
                + ", ip='" + ip + '\''
                + ", port=" + port
                + ", resource='" + resource + '\''
                + ", resourceMode=" + resourceMode
                + ", grade=" + grade
                + ", count=" + count
                + ", interval=" + interval
                + ", intervalUnit=" + intervalUnit
                + ", controlBehavior=" + controlBehavior
                + ", burst=" + burst
                + ", maxQueueingTimeoutMs=" + maxQueueingTimeoutMs
                + ", paramItem=" + paramItem
                + '}';
    }
}
