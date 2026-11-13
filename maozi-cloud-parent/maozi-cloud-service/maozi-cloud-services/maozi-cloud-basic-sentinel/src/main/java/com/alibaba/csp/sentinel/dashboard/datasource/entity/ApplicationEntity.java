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
 * Persistent entity of an application registered to the dashboard.
 *
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

    /** Get the last modified time. */
    public Date getGmtModified() {
        return gmtModified;
    }

    /** Set the last modified time. */
    public void setGmtModified(Date gmtModified) {
        this.gmtModified = gmtModified;
    }

    /** Get the app name. */
    public String getApp() {
        return app;
    }

    /** Set the app name. */
    public void setApp(String app) {
        this.app = app;
    }

    /** Get the type of the application. */
    public Integer getAppType() {
        return appType;
    }

    /** Set the type of the application. */
    public void setAppType(Integer appType) {
        this.appType = appType;
    }

    /** Get the active console address of the application. */
    public String getActiveConsole() {
        return activeConsole;
    }

    /** Get the last fetch time. */
    public Date getLastFetch() {
        return lastFetch;
    }

    /** Set the last fetch time. */
    public void setLastFetch(Date lastFetch) {
        this.lastFetch = lastFetch;
    }

    /** Set the active console address of the application. */
    public void setActiveConsole(String activeConsole) {
        this.activeConsole = activeConsole;
    }

    /** Convert to an {@link AppInfo} with the app name and app type of this entity. */
    public AppInfo toAppInfo() {
        return new AppInfo(app, appType);
    }

    /** Build a string with the fields of this entity except appType. */
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
