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
 * Persistent entity recording the last metric fetch position of a machine.
 *
 * @author leyou
 * @date 2026/11/22
 */
public class MetricPositionEntity extends BaseEntity {
    /** The IP address of the machine. */
    private String ip;
    /** The port Sentinel listens on in the application. */
    private int port;

    /** The hostname of the machine, a redundant field. */
    private String hostname;

    /** The latest timestamp fetched last time. */
    private Date lastFetch;

    /** Gets the IP address of the machine. */
    public String getIp() {
        return ip;
    }

    /** Sets the IP address of the machine. */
    public void setIp(String ip) {
        this.ip = ip;
    }

    /** Gets the port Sentinel listens on in the application. */
    public int getPort() {
        return port;
    }

    /** Sets the port Sentinel listens on in the application. */
    public void setPort(int port) {
        this.port = port;
    }

    /** Gets the hostname of the machine. */
    public String getHostname() {
        return hostname;
    }

    /** Sets the hostname of the machine. */
    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    /** Gets the latest timestamp fetched last time. */
    public Date getLastFetch() {
        return lastFetch;
    }

    /** Sets the latest timestamp fetched last time. */
    public void setLastFetch(Date lastFetch) {
        this.lastFetch = lastFetch;
    }

    /** Builds a string with all fields of this entity. */
    @Override
    public String toString() {
        return "MetricPositionEntity{" +
            "id=" + id +
            ", gmtCreate=" + gmtCreate +
            ", gmtModified=" + gmtModified +
            ", app='" + app + '\'' +
            ", ip='" + ip + '\'' +
            ", port=" + port +
            ", hostname='" + hostname + '\'' +
            ", lastFetch=" + lastFetch +
            '}';
    }
}
