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
 * Value Object for add gateway flow rule.
 *
 * @author cdfive
 * @since 1.7.0
 */
public class AddFlowRuleReqVo extends BaseFlowRuleReqVo {

    /** IP of the Sentinel client machine that the rule belongs to. */
    private String ip;

    /** Port of the Sentinel client machine that the rule belongs to. */
    private Integer port;

    /** Resource of the rule: route ID or API name, depending on resourceMode. */
    private String resource;

    /** Resource mode: 0 for route ID, 1 for custom API name (see SentinelGatewayConstants). */
    private Integer resourceMode;

    /** Returns the IP of the Sentinel client machine. */
    public String getIp() {
        return ip;
    }

    /** Sets the IP of the Sentinel client machine. */
    public void setIp(String ip) {
        this.ip = ip;
    }

    /** Returns the port of the Sentinel client machine. */
    public Integer getPort() {
        return port;
    }

    /** Sets the port of the Sentinel client machine. */
    public void setPort(Integer port) {
        this.port = port;
    }

    /** Returns the resource (route ID or API name). */
    public String getResource() {
        return resource;
    }

    /** Sets the resource (route ID or API name). */
    public void setResource(String resource) {
        this.resource = resource;
    }

    /** Returns the resource mode (0 route ID, 1 custom API name). */
    public Integer getResourceMode() {
        return resourceMode;
    }

    /** Sets the resource mode (0 route ID, 1 custom API name). */
    public void setResourceMode(Integer resourceMode) {
        this.resourceMode = resourceMode;
    }

    @Override
    public String toString() {
        return "AddFlowRuleReqVo{"
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
