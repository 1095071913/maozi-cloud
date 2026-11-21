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
package com.alibaba.csp.sentinel.dashboard.domain.vo;

/**
 * Base value object holding the realtime resource statistics fields shared by
 * resource view objects: resource name, thread count, QPS/RT counters of the
 * recent second and minute, and the visibility flag.
 *
 * @author leyou
 * @date 2026/11/22
 */
public abstract class BaseResourceMetricsVO {

    /** The name of the resource. */
    protected String resource;

    /** The concurrent thread count of the resource. */
    protected Integer threadNum;

    /** The QPS of passed requests. */
    protected Long passQps;

    /** The QPS of blocked requests. */
    protected Long blockQps;

    /** The total QPS of the resource. */
    protected Long totalQps;

    /** The average response time in ms. */
    protected Long averageRt;

    /** The QPS of exceptional requests. */
    protected Long exceptionQps;

    /** The passed request count in the recent minute. */
    protected Long oneMinutePass;

    /** The blocked request count in the recent minute. */
    protected Long oneMinuteBlock;

    /** The exception count in the recent minute. */
    protected Long oneMinuteException;

    /** The total request count in the recent minute. */
    protected Long oneMinuteTotal;

    /** Whether the node is visible. */
    protected boolean visible = true;

    /** Gets the resource name. */
    public String getResource() {
        return resource;
    }

    /** Sets the resource name. */
    public void setResource(String resource) {
        this.resource = resource;
    }

    /** Gets the concurrent thread count. */
    public Integer getThreadNum() {
        return threadNum;
    }

    /** Sets the concurrent thread count. */
    public void setThreadNum(Integer threadNum) {
        this.threadNum = threadNum;
    }

    /** Gets the QPS of passed requests. */
    public Long getPassQps() {
        return passQps;
    }

    /** Sets the QPS of passed requests. */
    public void setPassQps(Long passQps) {
        this.passQps = passQps;
    }

    /** Gets the QPS of blocked requests. */
    public Long getBlockQps() {
        return blockQps;
    }

    /** Sets the QPS of blocked requests. */
    public void setBlockQps(Long blockQps) {
        this.blockQps = blockQps;
    }

    /** Gets the total QPS. */
    public Long getTotalQps() {
        return totalQps;
    }

    /** Sets the total QPS. */
    public void setTotalQps(Long totalQps) {
        this.totalQps = totalQps;
    }

    /** Gets the average response time in ms. */
    public Long getAverageRt() {
        return averageRt;
    }

    /** Sets the average response time in ms. */
    public void setAverageRt(Long averageRt) {
        this.averageRt = averageRt;
    }

    /** Gets the QPS of exceptional requests. */
    public Long getExceptionQps() {
        return exceptionQps;
    }

    /** Sets the QPS of exceptional requests. */
    public void setExceptionQps(Long exceptionQps) {
        this.exceptionQps = exceptionQps;
    }

    /** Gets the passed request count in the recent minute. */
    public Long getOneMinutePass() {
        return oneMinutePass;
    }

    /** Sets the passed request count in the recent minute. */
    public void setOneMinutePass(Long oneMinutePass) {
        this.oneMinutePass = oneMinutePass;
    }

    /** Gets the blocked request count in the recent minute. */
    public Long getOneMinuteBlock() {
        return oneMinuteBlock;
    }

    /** Sets the blocked request count in the recent minute. */
    public void setOneMinuteBlock(Long oneMinuteBlock) {
        this.oneMinuteBlock = oneMinuteBlock;
    }

    /** Gets the exception count in the recent minute. */
    public Long getOneMinuteException() {
        return oneMinuteException;
    }

    /** Sets the exception count in the recent minute. */
    public void setOneMinuteException(Long oneMinuteException) {
        this.oneMinuteException = oneMinuteException;
    }

    /** Gets the total request count in the recent minute. */
    public Long getOneMinuteTotal() {
        return oneMinuteTotal;
    }

    /** Sets the total request count in the recent minute. */
    public void setOneMinuteTotal(Long oneMinuteTotal) {
        this.oneMinuteTotal = oneMinuteTotal;
    }

    /** Whether the node is visible. */
    public boolean isVisible() {
        return visible;
    }

    /** Sets whether the node is visible. */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }
}
