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

import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;

/**
 * 熔断降级规则实体。
 *
 * @author leyou
 * @date 2026/11/22
 */
public class DegradeRuleEntity extends BaseRuleEntity {

    /** 资源名。 */
    private String resource;
    /** 规则针对的调用来源应用。 */
    private String limitApp;
    /** 阈值，含义随熔断策略不同（平均 RT 毫秒数、异常比例或异常数）。 */
    private Double count;
    /** 降级时间窗口，单位秒。 */
    private Integer timeWindow;
    /** 熔断策略：0 平均响应时间（RT），1 异常比例，2 异常数。 */
    private Integer grade;
    /** 触发熔断的最小请求数。 */
    private Integer minRequestAmount;
    /** 慢调用比例阈值。 */
    private Double slowRatioThreshold;
    /** 统计时长，单位毫秒。 */
    private Integer statIntervalMs;

    /**
     * 根据 {@link DegradeRule} 构建携带指定应用名、IP、端口的实体。
     *
     * @param app  应用名
     * @param ip   机器 IP
     * @param port 机器端口
     * @param rule 熔断降级规则
     * @return 由规则字段填充而成的实体
     */
    public static DegradeRuleEntity fromDegradeRule(String app, String ip, Integer port, DegradeRule rule) {
        DegradeRuleEntity entity = new DegradeRuleEntity();
        entity.setApp(app);
        entity.setIp(ip);
        entity.setPort(port);
        entity.setResource(rule.getResource());
        entity.setLimitApp(rule.getLimitApp());
        entity.setCount(rule.getCount());
        entity.setTimeWindow(rule.getTimeWindow());
        entity.setGrade(rule.getGrade());
        entity.setMinRequestAmount(rule.getMinRequestAmount());
        entity.setSlowRatioThreshold(rule.getSlowRatioThreshold());
        entity.setStatIntervalMs(rule.getStatIntervalMs());
        return entity;
    }

    /** 获取资源名。 */
    public String getResource() {
        return resource;
    }

    /** 设置资源名。 */
    public void setResource(String resource) {
        this.resource = resource;
    }

    /** 获取规则针对的调用来源应用。 */
    public String getLimitApp() {
        return limitApp;
    }

    /** 设置规则针对的调用来源应用。 */
    public void setLimitApp(String limitApp) {
        this.limitApp = limitApp;
    }

    /** 获取阈值。 */
    public Double getCount() {
        return count;
    }

    /** 设置阈值。 */
    public void setCount(Double count) {
        this.count = count;
    }

    /** 获取降级时间窗口，单位秒。 */
    public Integer getTimeWindow() {
        return timeWindow;
    }

    /** 设置降级时间窗口，单位秒。 */
    public void setTimeWindow(Integer timeWindow) {
        this.timeWindow = timeWindow;
    }

    /** 获取熔断策略。 */
    public Integer getGrade() {
        return grade;
    }

    /** 设置熔断策略。 */
    public void setGrade(Integer grade) {
        this.grade = grade;
    }

    /** 获取触发熔断的最小请求数。 */
    public Integer getMinRequestAmount() {
        return minRequestAmount;
    }

    /** 设置触发熔断的最小请求数并返回当前对象。 */
    public DegradeRuleEntity setMinRequestAmount(Integer minRequestAmount) {
        this.minRequestAmount = minRequestAmount;
        return this;
    }

    /** 获取慢调用比例阈值。 */
    public Double getSlowRatioThreshold() {
        return slowRatioThreshold;
    }

    /** 设置慢调用比例阈值并返回当前对象。 */
    public DegradeRuleEntity setSlowRatioThreshold(Double slowRatioThreshold) {
        this.slowRatioThreshold = slowRatioThreshold;
        return this;
    }

    /** 获取统计时长，单位毫秒。 */
    public Integer getStatIntervalMs() {
        return statIntervalMs;
    }

    /** 设置统计时长，单位毫秒，并返回当前对象。 */
    public DegradeRuleEntity setStatIntervalMs(Integer statIntervalMs) {
        this.statIntervalMs = statIntervalMs;
        return this;
    }

    /**
     * 转换为 {@link DegradeRule}，minRequestAmount、slowRatioThreshold、statIntervalMs
     * 仅在非空时设置。
     */
    @Override
    public DegradeRule toRule() {
        DegradeRule rule = new DegradeRule();
        rule.setResource(resource);
        rule.setLimitApp(limitApp);
        rule.setCount(count);
        rule.setTimeWindow(timeWindow);
        rule.setGrade(grade);
        if (minRequestAmount != null) {
            rule.setMinRequestAmount(minRequestAmount);
        }
        if (slowRatioThreshold != null) {
            rule.setSlowRatioThreshold(slowRatioThreshold);
        }
        if (statIntervalMs != null) {
            rule.setStatIntervalMs(statIntervalMs);
        }

        return rule;
    }

    /** 返回实体的字符串表示。 */
    @Override
    public String toString() {
        return "DegradeRuleEntity{"
                + "id=" + id
                + ", app='" + app + '\''
                + ", ip='" + ip + '\''
                + ", port=" + port
                + ", resource='" + resource + '\''
                + ", limitApp='" + limitApp + '\''
                + ", count=" + count
                + ", timeWindow=" + timeWindow
                + ", grade=" + grade
                + ", minRequestAmount=" + minRequestAmount
                + ", slowRatioThreshold=" + slowRatioThreshold
                + ", statIntervalMs=" + statIntervalMs
                + ", gmtCreate=" + gmtCreate
                + ", gmtModified=" + gmtModified
                + '}';
    }
}
