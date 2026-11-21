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
 * 系统自适应保护规则实体。
 *
 * @author leyou
 * @date 2026/11/22
 */
public class SystemRuleEntity extends BaseRuleEntity {

    /** 系统 Load 最高阈值。 */
    private Double highestSystemLoad;
    /** 所有入口流量的平均 RT 阈值，单位毫秒。 */
    private Long avgRt;
    /** 并发线程数上限阈值。 */
    private Long maxThread;
    /** 所有入口流量的总 QPS 上限阈值。 */
    private Double qps;
    /** CPU 使用率最高阈值。 */
    private Double highestCpuUsage;

    /**
     * 根据 {@link SystemRule} 构建携带指定应用名、IP、端口的实体。
     *
     * @param app  应用名
     * @param ip   机器 IP
     * @param port 机器端口
     * @param rule 系统自适应保护规则
     * @return 由规则字段填充而成的实体
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

    /** 获取系统 Load 最高阈值。 */
    public Double getHighestSystemLoad() {
        return highestSystemLoad;
    }

    /** 设置系统 Load 最高阈值。 */
    public void setHighestSystemLoad(Double highestSystemLoad) {
        this.highestSystemLoad = highestSystemLoad;
    }

    /** 获取所有入口流量的平均 RT 阈值，单位毫秒。 */
    public Long getAvgRt() {
        return avgRt;
    }

    /** 设置所有入口流量的平均 RT 阈值，单位毫秒。 */
    public void setAvgRt(Long avgRt) {
        this.avgRt = avgRt;
    }

    /** 获取并发线程数上限阈值。 */
    public Long getMaxThread() {
        return maxThread;
    }

    /** 设置并发线程数上限阈值。 */
    public void setMaxThread(Long maxThread) {
        this.maxThread = maxThread;
    }

    /** 获取所有入口流量的总 QPS 上限阈值。 */
    public Double getQps() {
        return qps;
    }

    /** 设置所有入口流量的总 QPS 上限阈值。 */
    public void setQps(Double qps) {
        this.qps = qps;
    }

    /** 获取 CPU 使用率最高阈值。 */
    public Double getHighestCpuUsage() {
        return highestCpuUsage;
    }

    /** 设置 CPU 使用率最高阈值。 */
    public void setHighestCpuUsage(Double highestCpuUsage) {
        this.highestCpuUsage = highestCpuUsage;
    }

    /** 转换为 {@link SystemRule}，复制全部阈值字段。 */
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

    /** 返回实体的字符串表示。 */
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
