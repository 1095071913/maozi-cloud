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
package com.alibaba.csp.sentinel.dashboard.domain.cluster.state;

/**
 * 机器（IP 与命令端口）及其通用集群状态的配对。
 *
 * @author Eric Zhao
 * @date 2026/11/22
 * @since 1.4.1
 */
public class ClusterUniversalStatePairVO {

    /** 机器 IP 地址。 */
    private String ip;

    /** 机器命令端口。 */
    private Integer commandPort;

    /** 机器的通用集群状态。 */
    private ClusterUniversalStateVO state;

    /** 创建空的配对。 */
    public ClusterUniversalStatePairVO() {}

    /** 创建携带机器 IP、命令端口及其通用集群状态的配对。 */
    public ClusterUniversalStatePairVO(String ip, Integer commandPort, ClusterUniversalStateVO state) {
        this.ip = ip;
        this.commandPort = commandPort;
        this.state = state;
    }

    /** 获取 IP 地址。 */
    public String getIp() {
        return ip;
    }

    /** 设置 IP 地址并返回当前对象。 */
    public ClusterUniversalStatePairVO setIp(String ip) {
        this.ip = ip;
        return this;
    }

    /** 获取命令端口。 */
    public Integer getCommandPort() {
        return commandPort;
    }

    /** 设置命令端口并返回当前对象。 */
    public ClusterUniversalStatePairVO setCommandPort(Integer commandPort) {
        this.commandPort = commandPort;
        return this;
    }

    /** 获取通用集群状态。 */
    public ClusterUniversalStateVO getState() {
        return state;
    }

    /** 设置通用集群状态并返回当前对象。 */
    public ClusterUniversalStatePairVO setState(ClusterUniversalStateVO state) {
        this.state = state;
        return this;
    }

    /** 返回该 VO 的字符串表示。 */
    @Override
    public String toString() {
        return "ClusterUniversalStatePairVO{" +
            "ip='" + ip + '\'' +
            ", commandPort=" + commandPort +
            ", state=" + state +
            '}';
    }
}
