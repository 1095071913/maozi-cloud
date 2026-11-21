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
 * 实时监控页面展示的单个资源节点视图对象。
 *
 * @author leyou
 * @date 2026/11/22
 */
public class ResourceVO extends BaseResourceMetricsVO {
    /** 父树节点 id。 */
    private String parentTtId;

    /** 树节点 id。 */
    private String ttId;

    /** 资源通过请求的 QPS。 */
    private Long passRequestQps;

    /** 创建空的 VO。 */
    public ResourceVO() {
    }

    /** 将节点 VO 列表转换为资源 VO 列表，列表为 null 时返回 null。 */
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
     * 从指定根节点遍历资源树，将可见节点收集为 VO 列表，根节点为 null 时返回 null，
     * 根节点本身不会被收集。
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
     * 递归收集可见节点的 VO：节点自身或任一父节点可见即视为可见，根节点本身不收集。
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

    /** 获取父树节点 id。 */
    public String getParentTtId() {
        return parentTtId;
    }

    /** 设置父树节点 id。 */
    public void setParentTtId(String parentTtId) {
        this.parentTtId = parentTtId;
    }

    /** 获取树节点 id。 */
    public String getTtId() {
        return ttId;
    }

    /** 设置树节点 id。 */
    public void setTtId(String ttId) {
        this.ttId = ttId;
    }

    /** 获取通过请求的 QPS。 */
    public Long getPassRequestQps() {
        return passRequestQps;
    }

    /** 设置通过请求的 QPS。 */
    public void setPassRequestQps(Long passRequestQps) {
        this.passRequestQps = passRequestQps;
    }

    /** 返回该 VO 的字符串表示。 */
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
