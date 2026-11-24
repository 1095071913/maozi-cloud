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
package com.alibaba.csp.sentinel.dashboard.datasource.entity;

import java.util.Date;

/**
 * 记录机器监控指标最近拉取位置的持久化实体。
 *
 * @author leyou
 * @date 2026/11/22
 */
public class MetricPositionEntity extends BaseEntity {
    /** 机器 IP 地址。 */
    private String ip;
    /** 应用中 Sentinel 监听的端口。 */
    private Integer port;

    /** 机器主机名，冗余字段。 */
    private String hostname;

    /** 上次拉取到的最新时间戳。 */
    private Date lastFetch;

    /** 获取机器 IP 地址。 */
    public String getIp() {
        return ip;
    }

    /** 设置机器 IP 地址。 */
    public void setIp(String ip) {
        this.ip = ip;
    }

    /** 获取应用中 Sentinel 监听的端口。 */
    public Integer getPort() {
        return port;
    }

    /** 设置应用中 Sentinel 监听的端口。 */
    public void setPort(Integer port) {
        this.port = port;
    }

    /** 获取机器主机名。 */
    public String getHostname() {
        return hostname;
    }

    /** 设置机器主机名。 */
    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    /** 获取上次拉取到的最新时间戳。 */
    public Date getLastFetch() {
        return lastFetch;
    }

    /** 设置上次拉取到的最新时间戳。 */
    public void setLastFetch(Date lastFetch) {
        this.lastFetch = lastFetch;
    }

    /** 拼接全部字段构建字符串。 */
    @Override
    public String toString() {
        return "MetricPositionEntity{"
                + "id=" + id
                + ", gmtCreate=" + gmtCreate
                + ", gmtModified=" + gmtModified
                + ", app='" + app + '\''
                + ", ip='" + ip + '\''
                + ", port=" + port
                + ", hostname='" + hostname + '\''
                + ", lastFetch=" + lastFetch
                + '}';
    }
}
