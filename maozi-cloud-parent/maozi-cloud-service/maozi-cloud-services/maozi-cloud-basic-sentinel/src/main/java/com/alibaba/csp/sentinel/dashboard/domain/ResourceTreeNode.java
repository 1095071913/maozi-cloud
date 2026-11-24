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
import com.alibaba.csp.sentinel.util.StringUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 实时监控页面展示的资源树节点。
 *
 * @author leyou
 * @date 2026/11/22
 */
public class ResourceTreeNode extends BaseResourceMetricsVO {
    /** 节点 id。 */
    private String id;
    /** 父节点 id，根节点为 null 或空。 */
    private String parentId;

    /** 成功请求的 QPS。 */
    private Long successQps;

    /** 子节点列表。 */
    private List<ResourceTreeNode> children = new ArrayList<>();

    /**
     * 根据节点 VO 列表构建资源树：每个节点挂载到列表中先出现的父节点上，
     * 无父节点 id 的节点视为根节点。
     *
     * @param nodeVos 客户端上报的节点列表
     * @return 树的根节点，列表为 null 或空时返回 null
     */
    public static ResourceTreeNode fromNodeVoList(List<NodeVo> nodeVos) {
        if (nodeVos == null || nodeVos.isEmpty()) {
            return null;
        }
        ResourceTreeNode root = null;
        Map<String, ResourceTreeNode> map = new HashMap<>(nodeVos.size());
        for (NodeVo vo : nodeVos) {
            ResourceTreeNode node = fromNodeVo(vo);
            map.put(node.id, node);
            // 无父节点 id 的为真正的根节点
            if (StringUtil.isEmpty(node.parentId)) {
                root = node;
            } else {
                ResourceTreeNode parent = map.get(node.parentId);
                // 父节点尚未出现在列表中时不挂载，保持上游行为
                if (parent != null) {
                    parent.children.add(node);
                }
            }
        }
        return root;
    }

    /** 创建携带指定节点 VO 统计数据的树节点。 */
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
     * 以指定关键字（忽略大小写）搜索整棵树并更新每个节点的可见标记：
     * 节点自身或任一后代资源名命中即可见，关键字为 null 或空时全部可见。
     *
     * @param searchKey 搜索关键字
     */
    public void searchIgnoreCase(String searchKey) {
        search(this, searchKey == null ? null : searchKey.toLowerCase());
    }

    /**
     * 递归计算节点可见性：搜索关键字命中节点资源名或任一子节点可见时，该节点可见。
     */
    private boolean search(ResourceTreeNode node, String lowerSearchKey) {
        // 空关键字匹配全部资源
        node.visible = StringUtil.isEmpty(lowerSearchKey)
                || node.resource.toLowerCase().contains(lowerSearchKey);

        boolean childVisible = false;
        for (ResourceTreeNode child : node.children) {
            childVisible |= search(child, lowerSearchKey);
        }
        node.visible |= childVisible;
        return node.visible;
    }

    /** 获取节点 id。 */
    public String getId() {
        return id;
    }

    /** 设置节点 id。 */
    public void setId(String id) {
        this.id = id;
    }

    /** 获取父节点 id。 */
    public String getParentId() {
        return parentId;
    }

    /** 设置父节点 id。 */
    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    /** 获取成功请求的 QPS。 */
    public Long getSuccessQps() {
        return successQps;
    }

    /** 设置成功请求的 QPS。 */
    public void setSuccessQps(Long successQps) {
        this.successQps = successQps;
    }

    /** 获取子节点列表。 */
    public List<ResourceTreeNode> getChildren() {
        return children;
    }

    /** 设置子节点列表。 */
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
