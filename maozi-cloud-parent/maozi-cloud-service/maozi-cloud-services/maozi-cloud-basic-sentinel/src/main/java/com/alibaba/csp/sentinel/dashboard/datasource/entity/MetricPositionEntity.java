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
 */
public class MetricPositionEntity {
    /** The entity id. */
    private long id;
    /** The create time. */
    private Date gmtCreate;
    /** The last modify time. */
    private Date gmtModified;
    /** The application name. */
    private String app;
    /** The IP address of the machine. */
    private String ip;
    /** The port Sentinel listens on in the application. */
    private int port;

    /** The hostname of the machine, a redundant field. */
    private String hostname;

    /** The latest timestamp fetched last time. */
    private Date lastFetch;

    /** Get the entity id. */
    public long getId() {
        return id;
    }

    /** Set the entity id. */
    public void setId(long id) {
        this.id = id;
    }

    /** Get the create time. */
    public Date getGmtCreate() {
        return gmtCreate;
    }

    /** Set the create time. */
    public void setGmtCreate(Date gmtCreate) {
        this.gmtCreate = gmtCreate;
    }

    /** Get the last modify time. */
    public Date getGmtModified() {
        return gmtModified;
    }

    /** Set the last modify time. */
    public void setGmtModified(Date gmtModified) {
        this.gmtModified = gmtModified;
    }

    /** Get the application name. */
    public String getApp() {
        return app;
    }

    /** Set the application name. */
    public void setApp(String app) {
        this.app = app;
    }

    /** Get the IP address of the machine. */
    public String getIp() {
        return ip;
    }

    /** Set the IP address of the machine. */
    public void setIp(String ip) {
        this.ip = ip;
    }

    /** Get the port Sentinel listens on in the application. */
    public int getPort() {
        return port;
    }

    /** Set the port Sentinel listens on in the application. */
    public void setPort(int port) {
        this.port = port;
    }

    /** Get the hostname of the machine. */
    public String getHostname() {
        return hostname;
    }

    /** Set the hostname of the machine. */
    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    /** Get the latest timestamp fetched last time. */
    public Date getLastFetch() {
        return lastFetch;
    }

    /** Set the latest timestamp fetched last time. */
    public void setLastFetch(Date lastFetch) {
        this.lastFetch = lastFetch;
    }

    /** Build a string with all fields of this entity. */
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
