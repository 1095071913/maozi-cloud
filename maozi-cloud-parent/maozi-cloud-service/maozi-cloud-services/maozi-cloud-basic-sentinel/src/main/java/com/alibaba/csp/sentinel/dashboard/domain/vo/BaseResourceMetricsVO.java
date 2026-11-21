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
 * 资源实时统计指标基类，持有各资源视图对象共用的统计字段：资源名、并发线程数、
 * 秒级与分钟级的 QPS/RT 计数，以及可见标记。
 *
 * @author leyou
 * @date 2026/11/22
 */
public abstract class BaseResourceMetricsVO {

    /** 资源名。 */
    protected String resource;

    /** 资源的并发线程数。 */
    protected Integer threadNum;

    /** 通过请求的 QPS。 */
    protected Long passQps;

    /** 拒绝请求的 QPS。 */
    protected Long blockQps;

    /** 资源的总 QPS。 */
    protected Long totalQps;

    /** 平均响应时间，单位毫秒。 */
    protected Long averageRt;

    /** 异常请求的 QPS。 */
    protected Long exceptionQps;

    /** 最近一分钟通过的请求数。 */
    protected Long oneMinutePass;

    /** 最近一分钟拒绝的请求数。 */
    protected Long oneMinuteBlock;

    /** 最近一分钟的异常请求数。 */
    protected Long oneMinuteException;

    /** 最近一分钟的总请求数。 */
    protected Long oneMinuteTotal;

    /** 节点是否可见。 */
    protected boolean visible = true;

    /** 获取资源名。 */
    public String getResource() {
        return resource;
    }

    /** 设置资源名。 */
    public void setResource(String resource) {
        this.resource = resource;
    }

    /** 获取并发线程数。 */
    public Integer getThreadNum() {
        return threadNum;
    }

    /** 设置并发线程数。 */
    public void setThreadNum(Integer threadNum) {
        this.threadNum = threadNum;
    }

    /** 获取通过请求的 QPS。 */
    public Long getPassQps() {
        return passQps;
    }

    /** 设置通过请求的 QPS。 */
    public void setPassQps(Long passQps) {
        this.passQps = passQps;
    }

    /** 获取拒绝请求的 QPS。 */
    public Long getBlockQps() {
        return blockQps;
    }

    /** 设置拒绝请求的 QPS。 */
    public void setBlockQps(Long blockQps) {
        this.blockQps = blockQps;
    }

    /** 获取总 QPS。 */
    public Long getTotalQps() {
        return totalQps;
    }

    /** 设置总 QPS。 */
    public void setTotalQps(Long totalQps) {
        this.totalQps = totalQps;
    }

    /** 获取平均响应时间，单位毫秒。 */
    public Long getAverageRt() {
        return averageRt;
    }

    /** 设置平均响应时间，单位毫秒。 */
    public void setAverageRt(Long averageRt) {
        this.averageRt = averageRt;
    }

    /** 获取异常请求的 QPS。 */
    public Long getExceptionQps() {
        return exceptionQps;
    }

    /** 设置异常请求的 QPS。 */
    public void setExceptionQps(Long exceptionQps) {
        this.exceptionQps = exceptionQps;
    }

    /** 获取最近一分钟通过的请求数。 */
    public Long getOneMinutePass() {
        return oneMinutePass;
    }

    /** 设置最近一分钟通过的请求数。 */
    public void setOneMinutePass(Long oneMinutePass) {
        this.oneMinutePass = oneMinutePass;
    }

    /** 获取最近一分钟拒绝的请求数。 */
    public Long getOneMinuteBlock() {
        return oneMinuteBlock;
    }

    /** 设置最近一分钟拒绝的请求数。 */
    public void setOneMinuteBlock(Long oneMinuteBlock) {
        this.oneMinuteBlock = oneMinuteBlock;
    }

    /** 获取最近一分钟的异常请求数。 */
    public Long getOneMinuteException() {
        return oneMinuteException;
    }

    /** 设置最近一分钟的异常请求数。 */
    public void setOneMinuteException(Long oneMinuteException) {
        this.oneMinuteException = oneMinuteException;
    }

    /** 获取最近一分钟的总请求数。 */
    public Long getOneMinuteTotal() {
        return oneMinuteTotal;
    }

    /** 设置最近一分钟的总请求数。 */
    public void setOneMinuteTotal(Long oneMinuteTotal) {
        this.oneMinuteTotal = oneMinuteTotal;
    }

    /** 节点是否可见。 */
    public boolean isVisible() {
        return visible;
    }

    /** 设置节点是否可见。 */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }
}
