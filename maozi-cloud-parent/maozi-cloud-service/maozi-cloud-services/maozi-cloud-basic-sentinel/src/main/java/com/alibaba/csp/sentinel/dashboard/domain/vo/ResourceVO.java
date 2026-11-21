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
 * View object of one resource node on the realtime monitoring page.
 *
 * @author leyou
 * @date 2026/11/22
 */
public class ResourceVO extends BaseResourceMetricsVO {
    /** The parent tree node ID. */
    private String parentTtId;

    /** The tree node ID. */
    private String ttId;

    /** The pass request QPS of the resource. */
    private Long passRequestQps;

    /** Create an empty vo. */
    public ResourceVO() {
    }

    /** Convert a list of node VOs to a list of resource VOs, or null if the given list is null. */
    public static List<ResourceVO> fromNodeVoList(List<NodeVo> nodeVos) {
        if (nodeVos == null) {
            return null;
        }
        List<ResourceVO> list = new ArrayList<>(nodeVos.size());
        for (NodeVo nodeVo : nodeVos) {
            ResourceVO vo = new ResourceVO();
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
    public static List<ResourceVO> fromResourceTreeNode(ResourceTreeNode root) {
        if (root == null) {
            return null;
        }
        List<ResourceVO> list = new ArrayList<>();
        visit(root, list, false, true);
        return list;
    }

    /**
     * Recursively collects VOs of the visible nodes into the list. A node is visible
     * when itself or one of its parents is visible; the root node is never visible.
     */
    private static void visit(ResourceTreeNode node, List<ResourceVO> list, boolean parentVisible, boolean isRoot) {
        boolean visible = !isRoot && (node.isVisible() || parentVisible);
        if (visible) {
            ResourceVO vo = new ResourceVO();
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

    /** Get the pass request QPS. */
    public Long getPassRequestQps() {
        return passRequestQps;
    }

    /** Set the pass request QPS. */
    public void setPassRequestQps(Long passRequestQps) {
        this.passRequestQps = passRequestQps;
    }

    /** Return a string representation of this VO. */
    @Override
    public String toString() {
        return "ResourceVO{"
                + "parentTtId='" + parentTtId + '\''
                + ", ttId='" + ttId + '\''
                + ", resource='" + resource + '\''
                + ", threadNum=" + threadNum
                + ", passQps=" + passQps
                + ", blockQps=" + blockQps
                + ", totalQps=" + totalQps
                + ", averageRt=" + averageRt
                + ", passRequestQps=" + passRequestQps
                + ", exceptionQps=" + exceptionQps
                + ", oneMinutePass=" + oneMinutePass
                + ", oneMinuteBlock=" + oneMinuteBlock
                + ", oneMinuteException=" + oneMinuteException
                + ", oneMinuteTotal=" + oneMinuteTotal
                + ", visible=" + visible
                + '}';
    }
}
