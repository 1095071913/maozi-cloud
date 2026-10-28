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

import com.alibaba.csp.sentinel.dashboard.discovery.AppInfo;

import java.util.Date;

/**
 * @author leyou
 */
public class ApplicationEntity {

    /** The primary key id. */
    private Long id;
    /** The create time. */
    private Date gmtCreate;
    /** The last modified time. */
    private Date gmtModified;
    /** The app name. */
    private String app;
    /** The type of the application. */
    private Integer appType;
    /** The active console address of the application. */
    private String activeConsole;
    /** The last fetch time. */
    private Date lastFetch;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Date getGmtCreate() {
        return gmtCreate;
    }

    public void setGmtCreate(Date gmtCreate) {
        this.gmtCreate = gmtCreate;
    }

    public Date getGmtModified() {
        return gmtModified;
    }

    public void setGmtModified(Date gmtModified) {
        this.gmtModified = gmtModified;
    }

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public Integer getAppType() {
        return appType;
    }

    public void setAppType(Integer appType) {
        this.appType = appType;
    }

    public String getActiveConsole() {
        return activeConsole;
    }

    public Date getLastFetch() {
        return lastFetch;
    }

    public void setLastFetch(Date lastFetch) {
        this.lastFetch = lastFetch;
    }

    public void setActiveConsole(String activeConsole) {
        this.activeConsole = activeConsole;
    }

    public AppInfo toAppInfo() {
        return new AppInfo(app, appType);
    }

    @Override
    public String toString() {
        return "ApplicationEntity{" +
            "id=" + id +
            ", gmtCreate=" + gmtCreate +
            ", gmtModified=" + gmtModified +
            ", app='" + app + '\'' +
            ", activeConsole='" + activeConsole + '\'' +
            ", lastFetch=" + lastFetch +
            '}';
    }
}
