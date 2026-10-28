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
package com.alibaba.csp.sentinel.dashboard.domain;

import com.alibaba.csp.sentinel.command.vo.NodeVo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author leyou
 */
public class ResourceTreeNode {
    /** The id of the node. */
    private String id;
    /** The id of the parent node, null or empty for the root node. */
    private String parentId;
    /** The name of the resource. */
    private String resource;

    /** The concurrent thread count of the resource. */
    private Integer threadNum;
    /** The QPS of passed requests. */
    private Long passQps;
    /** The QPS of blocked requests. */
    private Long blockQps;
    /** The total QPS of the resource. */
    private Long totalQps;
    /** The average response time in ms. */
    private Long averageRt;
    /** The QPS of successful requests. */
    private Long successQps;
    /** The QPS of exceptional requests. */
    private Long exceptionQps;
    /** The passed request count in the recent minute. */
    private Long oneMinutePass;
    /** The blocked request count in the recent minute. */
    private Long oneMinuteBlock;
    /** The exception count in the recent minute. */
    private Long oneMinuteException;
    /** The total request count in the recent minute. */
    private Long oneMinuteTotal;

    /** Whether the node is visible after search. */
    private boolean visible = true;

    /** The child nodes of this node. */
    private List<ResourceTreeNode> children = new ArrayList<>();

    /**
     * Build a resource tree from the list of node VOs. Each node is attached to
     * its parent node that appeared earlier in the list, and the node without a
     * parent id is treated as the root.
     *
     * @param nodeVos the node list reported by the client
     * @return the root node of the tree, or null if the list is null or empty
     */
    public static ResourceTreeNode fromNodeVoList(List<NodeVo> nodeVos) {
        if (nodeVos == null || nodeVos.isEmpty()) {
            return null;
        }
        ResourceTreeNode root = null;
        Map<String, ResourceTreeNode> map = new HashMap<>();
        for (NodeVo vo : nodeVos) {
            ResourceTreeNode node = fromNodeVo(vo);
            map.put(node.id, node);
            // real root
            if (node.parentId == null || node.parentId.isEmpty()) {
                root = node;
            } else if (map.containsKey(node.parentId)) {
                map.get(node.parentId).children.add(node);
            } else {
                // impossible
            }
        }
        return root;
    }

    /** Create a tree node carrying the statistics of the given node VO. */
    public static ResourceTreeNode fromNodeVo(NodeVo vo) {
        ResourceTreeNode node = new ResourceTreeNode();
        node.id = vo.getId();
        node.parentId = vo.getParentId();
        node.resource = vo.getResource();
        node.threadNum = vo.getThreadNum();
        node.passQps = vo.getPassQps();
        node.blockQps = vo.getBlockQps();
        node.totalQps = vo.getTotalQps();
        node.averageRt = vo.getAverageRt();
        node.successQps = vo.getSuccessQps();
        node.exceptionQps = vo.getExceptionQps();
        node.oneMinutePass = vo.getOneMinutePass();
        node.oneMinuteBlock = vo.getOneMinuteBlock();
        node.oneMinuteException = vo.getOneMinuteException();
        node.oneMinuteTotal = vo.getOneMinuteTotal();
        return node;
    }

    /**
     * Search the whole tree with the given key (case-insensitive) and update the
     * visible flag of every node accordingly: a node is visible when it or any
     * of its descendants matches, and a null or empty key matches all resources.
     */
    public void searchIgnoreCase(String searchKey) {
        search(this, searchKey);
    }

    /**
     * This node is visible only when searchKey matches this.resource or at least
     * one of this's children is visible
     */
    private boolean search(ResourceTreeNode node, String searchKey) {
        // empty matches all
        if (searchKey == null || searchKey.isEmpty() ||
            node.resource.toLowerCase().contains(searchKey.toLowerCase())) {
            node.visible = true;
        } else {
            node.visible = false;
        }

        boolean found = false;
        for (ResourceTreeNode c : node.children) {
            found |= search(c, searchKey);
        }
        node.visible |= found;
        return node.visible;
    }

    /** Get the id of the node. */
    public String getId() {
        return id;
    }

    /** Set the id of the node. */
    public void setId(String id) {
        this.id = id;
    }

    /** Get the id of the parent node. */
    public String getParentId() {
        return parentId;
    }

    /** Set the id of the parent node. */
    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    /** Get the resource name. */
    public String getResource() {
        return resource;
    }

    /** Set the resource name. */
    public void setResource(String resource) {
        this.resource = resource;
    }

    /** Get the concurrent thread count. */
    public Integer getThreadNum() {
        return threadNum;
    }

    /** Set the concurrent thread count. */
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

    /** Get the average response time in ms. */
    public Long getAverageRt() {
        return averageRt;
    }

    /** Set the average response time in ms. */
    public void setAverageRt(Long averageRt) {
        this.averageRt = averageRt;
    }

    /** Get the QPS of successful requests. */
    public Long getSuccessQps() {
        return successQps;
    }

    /** Set the QPS of successful requests. */
    public void setSuccessQps(Long successQps) {
        this.successQps = successQps;
    }

    /** Get the QPS of exceptional requests. */
    public Long getExceptionQps() {
        return exceptionQps;
    }

    /** Set the QPS of exceptional requests. */
    public void setExceptionQps(Long exceptionQps) {
        this.exceptionQps = exceptionQps;
    }

    /** Get the passed request count in the recent minute. */
    public Long getOneMinutePass() {
        return oneMinutePass;
    }

    /** Set the passed request count in the recent minute. */
    public void setOneMinutePass(Long oneMinutePass) {
        this.oneMinutePass = oneMinutePass;
    }

    /** Get the blocked request count in the recent minute. */
    public Long getOneMinuteBlock() {
        return oneMinuteBlock;
    }

    /** Set the blocked request count in the recent minute. */
    public void setOneMinuteBlock(Long oneMinuteBlock) {
        this.oneMinuteBlock = oneMinuteBlock;
    }

    /** Get the exception count in the recent minute. */
    public Long getOneMinuteException() {
        return oneMinuteException;
    }

    /** Set the exception count in the recent minute. */
    public void setOneMinuteException(Long oneMinuteException) {
        this.oneMinuteException = oneMinuteException;
    }

    /** Get the total request count in the recent minute. */
    public Long getOneMinuteTotal() {
        return oneMinuteTotal;
    }

    /** Set the total request count in the recent minute. */
    public void setOneMinuteTotal(Long oneMinuteTotal) {
        this.oneMinuteTotal = oneMinuteTotal;
    }

    /** Whether the node is visible. */
    public boolean isVisible() {
        return visible;
    }

    /** Set whether the node is visible. */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    /** Get the child nodes. */
    public List<ResourceTreeNode> getChildren() {
        return children;
    }

    /** Set the child nodes. */
    public void setChildren(List<ResourceTreeNode> children) {
        this.children = children;
    }
}

