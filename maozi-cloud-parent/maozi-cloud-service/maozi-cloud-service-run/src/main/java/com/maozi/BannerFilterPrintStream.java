package com.maozi;

import java.io.PrintStream;
import java.util.List;

/**
 * 启动 banner 过滤输出流
 * <p>
 * 全局唯一的标准输出包装器，由 {@link BaseApplication} 在启动最初阶段安装，
 * 过滤特征由各组件模块（如 Dubbo、SnailJob 配置模块）通过
 * {@code BaseApplication.addBannerFilter} 注册到共享特征列表。
 * 输出内容命中任一特征片段时整体丢弃，其余输出原样透传给原标准输出流。
 * </p>
 * <p>
 * 注意：仅允许存在这一层包装。多层 PrintStream 嵌套时，外层的
 * {@code print/println} 最终以字节流写入底层输出流，会绕过内层对
 * {@code print/println(String)} 的重写，导致内层过滤失效。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
public class BannerFilterPrintStream extends PrintStream {

    /** banner 过滤特征列表（来自 BaseApplication 的共享字段），过滤时实时读取，支持启动过程中动态注册 */
    private final List<String> bannerFilters;

    /**
     * 以原标准输出流为底层输出构造过滤器
     *
     * @param delegate 原标准输出流
     * @param bannerFilters banner 过滤特征列表
     */
    public BannerFilterPrintStream(PrintStream delegate, List<String> bannerFilters) {
        super(delegate, true);
        this.bannerFilters = bannerFilters;
    }

    /**
     * 输出字符串（不换行），命中 banner 特征时丢弃，其余透传
     *
     * @param x 待输出字符串
     */
    @Override
    public void print(String x) {
        if (isBanner(x)) {
            return;
        }
        super.print(x);
    }

    /**
     * 输出一行字符串（换行），命中 banner 特征时丢弃，其余透传
     *
     * @param x 待输出字符串
     */
    @Override
    public void println(String x) {
        if (isBanner(x)) {
            return;
        }
        super.println(x);
    }

    /**
     * 判断输出内容是否命中任一 banner 过滤特征
     *
     * @param x 待输出字符串
     * @return 命中返回 true（含 null 安全处理，null 不视为 banner）
     */
    private boolean isBanner(String x) {
        if (x == null || x.isEmpty()) {
            return false;
        }
        for (String flag : bannerFilters) {
            if (x.contains(flag)) {
                return true;
            }
        }
        return false;
    }

}
