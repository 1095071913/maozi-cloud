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
package com.alibaba.csp.sentinel.dashboard.datasource.entity.rule;

import com.alibaba.csp.sentinel.slots.system.SystemRule;

/**
 * Entity for a system adaptive protection rule.
 *
 * @author leyou
 */
public class SystemRuleEntity extends BaseRuleEntity {

    /** The highest system load threshold. */
    private Double highestSystemLoad;
    /** The average RT (ms) threshold for all inbound requests. */
    private Long avgRt;
    /** The max concurrent thread count threshold. */
    private Long maxThread;
    /** The max total QPS of all inbound requests. */
    private Double qps;
    /** The highest CPU usage threshold. */
    private Double highestCpuUsage;

    /**
     * Builds an entity from the given {@link SystemRule}, carrying the given app, ip and port.
     */
    public static SystemRuleEntity fromSystemRule(String app, String ip, Integer port, SystemRule rule) {
        SystemRuleEntity entity = new SystemRuleEntity();
        entity.setApp(app);
        entity.setIp(ip);
        entity.setPort(port);
        entity.setHighestSystemLoad(rule.getHighestSystemLoad());
        entity.setHighestCpuUsage(rule.getHighestCpuUsage());
        entity.setAvgRt(rule.getAvgRt());
        entity.setMaxThread(rule.getMaxThread());
        entity.setQps(rule.getQps());
        return entity;
    }

    /** Gets the highest system load threshold. */
    public Double getHighestSystemLoad() {
        return highestSystemLoad;
    }

    /** Sets the highest system load threshold. */
    public void setHighestSystemLoad(Double highestSystemLoad) {
        this.highestSystemLoad = highestSystemLoad;
    }

    /** Gets the average RT (ms) threshold for all inbound requests. */
    public Long getAvgRt() {
        return avgRt;
    }

    /** Sets the average RT (ms) threshold for all inbound requests. */
    public void setAvgRt(Long avgRt) {
        this.avgRt = avgRt;
    }

    /** Gets the max concurrent thread count threshold. */
    public Long getMaxThread() {
        return maxThread;
    }

    /** Sets the max concurrent thread count threshold. */
    public void setMaxThread(Long maxThread) {
        this.maxThread = maxThread;
    }

    /** Gets the max total QPS of all inbound requests. */
    public Double getQps() {
        return qps;
    }

    /** Sets the max total QPS of all inbound requests. */
    public void setQps(Double qps) {
        this.qps = qps;
    }

    /** Gets the highest CPU usage threshold. */
    public Double getHighestCpuUsage() {
        return highestCpuUsage;
    }

    /** Sets the highest CPU usage threshold. */
    public void setHighestCpuUsage(Double highestCpuUsage) {
        this.highestCpuUsage = highestCpuUsage;
    }

    /** Converts to a {@link SystemRule} with all threshold fields copied. */
    @Override
    public SystemRule toRule() {
        SystemRule rule = new SystemRule();
        rule.setHighestSystemLoad(highestSystemLoad);
        rule.setAvgRt(avgRt);
        rule.setMaxThread(maxThread);
        rule.setQps(qps);
        rule.setHighestCpuUsage(highestCpuUsage);
        return rule;
    }

    @Override
    public String toString() {
        return "SystemRuleEntity{"
                + "id=" + id
                + ", app='" + app + '\''
                + ", ip='" + ip + '\''
                + ", port=" + port
                + ", highestSystemLoad=" + highestSystemLoad
                + ", avgRt=" + avgRt
                + ", maxThread=" + maxThread
                + ", qps=" + qps
                + ", highestCpuUsage=" + highestCpuUsage
                + ", gmtCreate=" + gmtCreate
                + ", gmtModified=" + gmtModified
                + '}';
    }
}
