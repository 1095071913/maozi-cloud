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
import com.alibaba.csp.sentinel.dashboard.domain.vo.BaseResourceMetricsVO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Node of the resource tree shown on the realtime monitoring page.
 *
 * @author leyou
 * @date 2026/11/22
 */
public class ResourceTreeNode extends BaseResourceMetricsVO {
    /** The id of the node. */
    private String id;
    /** The id of the parent node, null or empty for the root node. */
    private String parentId;

    /** The QPS of successful requests. */
    private Long successQps;

    /** The child nodes of this node. */
    private List<ResourceTreeNode> children = new ArrayList<>();

    /**
     * Builds a resource tree from the list of node VOs. Each node is attached to
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

    /** Creates a tree node carrying the statistics of the given node VO. */
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
     * Searches the whole tree with the given key (case-insensitive) and updates the
     * visible flag of every node accordingly: a node is visible when it or any
     * of its descendants matches, and a null or empty key matches all resources.
     */
    public void searchIgnoreCase(String searchKey) {
        search(this, searchKey);
    }

    /**
     * Recursively computes visibility: the node is visible when the search key
     * matches its resource or at least one child is visible.
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

    /** Gets the id of the node. */
    public String getId() {
        return id;
    }

    /** Sets the id of the node. */
    public void setId(String id) {
        this.id = id;
    }

    /** Gets the id of the parent node. */
    public String getParentId() {
        return parentId;
    }

    /** Sets the id of the parent node. */
    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    /** Gets the QPS of successful requests. */
    public Long getSuccessQps() {
        return successQps;
    }

    /** Sets the QPS of successful requests. */
    public void setSuccessQps(Long successQps) {
        this.successQps = successQps;
    }

    /** Gets the child nodes. */
    public List<ResourceTreeNode> getChildren() {
        return children;
    }

    /** Sets the child nodes. */
    public void setChildren(List<ResourceTreeNode> children) {
        this.children = children;
    }

    @Override
    public String toString() {
        return "ResourceTreeNode{"
                + "id='" + id + '\''
                + ", parentId='" + parentId + '\''
                + ", resource='" + resource + '\''
                + ", threadNum=" + threadNum
                + ", passQps=" + passQps
                + ", blockQps=" + blockQps
                + ", totalQps=" + totalQps
                + ", averageRt=" + averageRt
                + ", successQps=" + successQps
                + ", exceptionQps=" + exceptionQps
                + ", oneMinutePass=" + oneMinutePass
                + ", oneMinuteBlock=" + oneMinuteBlock
                + ", oneMinuteException=" + oneMinuteException
                + ", oneMinuteTotal=" + oneMinuteTotal
                + ", visible=" + visible
                + ", children=" + children
                + '}';
    }
}
