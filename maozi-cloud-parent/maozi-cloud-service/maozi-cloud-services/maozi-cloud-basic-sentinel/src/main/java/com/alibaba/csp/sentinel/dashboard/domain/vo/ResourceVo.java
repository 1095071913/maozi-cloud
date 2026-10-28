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

import com.alibaba.csp.sentinel.command.vo.NodeVo;
import com.alibaba.csp.sentinel.dashboard.domain.ResourceTreeNode;

import java.util.ArrayList;
import java.util.List;

/**
 * @author leyou
 */
public class ResourceVo {
    /** The parent tree node ID. */
    private String parentTtId;

    /** The tree node ID. */
    private String ttId;

    /** The resource name. */
    private String resource;

    /** The current thread count of the resource. */
    private Integer threadNum;

    /** The QPS of passed requests. */
    private Long passQps;

    /** The QPS of blocked requests. */
    private Long blockQps;

    /** The total QPS of the resource. */
    private Long totalQps;

    /** The average response time of the resource. */
    private Long averageRt;

    /** The pass request QPS of the resource. */
    private Long passRequestQps;

    /** The QPS of exceptions. */
    private Long exceptionQps;

    /** The pass count in the last minute. */
    private Long oneMinutePass;

    /** The block count in the last minute. */
    private Long oneMinuteBlock;

    /** The exception count in the last minute. */
    private Long oneMinuteException;

    /** The total count in the last minute. */
    private Long oneMinuteTotal;

    /** Whether this node is visible in the resource tree. */
    private boolean visible = true;

    /** Create an empty vo. */
    public ResourceVo() {
    }

    /** Convert a list of node VOs to a list of resource VOs, or null if the given list is null. */
    public static List<ResourceVo> fromNodeVoList(List<NodeVo> nodeVos) {
        if (nodeVos == null) {
            return null;
        }
        List<ResourceVo> list = new ArrayList<>();
        for (NodeVo nodeVo : nodeVos) {
            ResourceVo vo = new ResourceVo();
            vo.parentTtId = nodeVo.getParentId();
            vo.ttId = nodeVo.getId();
            vo.resource = nodeVo.getResource();
            vo.threadNum = nodeVo.getThreadNum();
            vo.passQps = nodeVo.getPassQps();
            vo.blockQps = nodeVo.getBlockQps();
            vo.totalQps = nodeVo.getTotalQps();
            vo.averageRt = nodeVo.getAverageRt();
            vo.exceptionQps = nodeVo.getExceptionQps();
            vo.oneMinutePass = nodeVo.getOneMinutePass();
            vo.oneMinuteBlock = nodeVo.getOneMinuteBlock();
            vo.oneMinuteException = nodeVo.getOneMinuteException();
            vo.oneMinuteTotal = nodeVo.getOneMinuteTotal();
            list.add(vo);
        }
        return list;
    }

    /**
     * Visit the resource tree from the given root node and collect the visible
     * nodes into a list of VOs, or null if the given root is null. The root node
     * itself is never collected.
     */
    public static List<ResourceVo> fromResourceTreeNode(ResourceTreeNode root) {
        if (root == null) {
            return null;
        }
        List<ResourceVo> list = new ArrayList<>();
        visit(root, list, false, true);
        //if(!list.isEmpty()){
        //    list.remove(0);
        //}
        return list;
    }

    /**
     * This node is visible when this.visible==true or one of this's parents is visible,
     * root node is always invisible.
     */
    private static void visit(ResourceTreeNode node, List<ResourceVo> list, boolean parentVisible, boolean isRoot) {
        boolean visible = !isRoot && (node.isVisible() || parentVisible);
        //boolean visible = node.isVisible();
        if (visible) {
            ResourceVo vo = new ResourceVo();
            vo.parentTtId = node.getParentId();
            vo.ttId = node.getId();
            vo.resource = node.getResource();
            vo.threadNum = node.getThreadNum();
            vo.passQps = node.getPassQps();
            vo.blockQps = node.getBlockQps();
            vo.totalQps = node.getTotalQps();
            vo.averageRt = node.getAverageRt();
            vo.exceptionQps = node.getExceptionQps();
            vo.oneMinutePass = node.getOneMinutePass();
            vo.oneMinuteBlock = node.getOneMinuteBlock();
            vo.oneMinuteException = node.getOneMinuteException();
            vo.oneMinuteTotal = node.getOneMinuteTotal();
            vo.visible = node.isVisible();
            list.add(vo);
        }
        for (ResourceTreeNode c : node.getChildren()) {
            visit(c, list, visible, false);
        }
    }

    /** Get the parent tree node ID. */
    public String getParentTtId() {
        return parentTtId;
    }

    /** Set the parent tree node ID. */
    public void setParentTtId(String parentTtId) {
        this.parentTtId = parentTtId;
    }

    /** Get the tree node ID. */
    public String getTtId() {
        return ttId;
    }

    /** Set the tree node ID. */
    public void setTtId(String ttId) {
        this.ttId = ttId;
    }

    /** Get the resource name. */
    public String getResource() {
        return resource;
    }

    /** Set the resource name. */
    public void setResource(String resource) {
        this.resource = resource;
    }

    /** Get the current thread count. */
    public Integer getThreadNum() {
        return threadNum;
    }

    /** Set the current thread count. */
    public void setThreadNum(Integer threadNum) {
        this.threadNum = threadNum;
    }

    /** Get the QPS of passed requests. */
    public Long getPassQps() {
        return passQps;
    }

    /** Set the QPS of passed requests. */
    public void setPassQps(Long passQps) {
        this.passQps = passQps;
    }

    /** Get the QPS of blocked requests. */
    public Long getBlockQps() {
        return blockQps;
    }

    /** Set the QPS of blocked requests. */
    public void setBlockQps(Long blockQps) {
        this.blockQps = blockQps;
    }

    /** Get the total QPS. */
    public Long getTotalQps() {
        return totalQps;
    }

    /** Set the total QPS. */
    public void setTotalQps(Long totalQps) {
        this.totalQps = totalQps;
    }

    /** Get the average response time. */
    public Long getAverageRt() {
        return averageRt;
    }

    /** Set the average response time. */
    public void setAverageRt(Long averageRt) {
        this.averageRt = averageRt;
    }

    /** Get the pass request QPS. */
    public Long getPassRequestQps() {
        return passRequestQps;
    }

    /** Set the pass request QPS. */
    public void setPassRequestQps(Long passRequestQps) {
        this.passRequestQps = passRequestQps;
    }

    /** Get the QPS of exceptions. */
    public Long getExceptionQps() {
        return exceptionQps;
    }

    /** Set the QPS of exceptions. */
    public void setExceptionQps(Long exceptionQps) {
        this.exceptionQps = exceptionQps;
    }

    /** Get the exception count in the last minute. */
    public Long getOneMinuteException() {
        return oneMinuteException;
    }

    /** Set the exception count in the last minute. */
    public void setOneMinuteException(Long oneMinuteException) {
        this.oneMinuteException = oneMinuteException;
    }

    /** Get the pass count in the last minute. */
    public Long getOneMinutePass() {
        return oneMinutePass;
    }

    /** Set the pass count in the last minute. */
    public void setOneMinutePass(Long oneMinutePass) {
        this.oneMinutePass = oneMinutePass;
    }

    /** Get the block count in the last minute. */
    public Long getOneMinuteBlock() {
        return oneMinuteBlock;
    }

    /** Set the block count in the last minute. */
    public void setOneMinuteBlock(Long oneMinuteBlock) {
        this.oneMinuteBlock = oneMinuteBlock;
    }

    /** Get the total count in the last minute. */
    public Long getOneMinuteTotal() {
        return oneMinuteTotal;
    }

    /** Set the total count in the last minute. */
    public void setOneMinuteTotal(Long oneMinuteTotal) {
        this.oneMinuteTotal = oneMinuteTotal;
    }

    /** Whether this node is visible in the resource tree. */
    public boolean isVisible() {
        return visible;
    }

    /** Set whether this node is visible in the resource tree. */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }
}
