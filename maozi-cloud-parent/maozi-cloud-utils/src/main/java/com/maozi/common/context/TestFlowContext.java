package com.maozi.common.context;

import com.maozi.common.ObjectUtil;

/**
 * 测试流量标记上下文
 * <p>
 * 存储当前线程的测试流量标记（true 表示测试流量），
 * 请求头字符串为 "true" 时标记为测试流量。
 * </p>
 *
 * @author maozi
 * @date 2026/11/13
 */
public class TestFlowContext extends AbstractRequestContextHolder<Boolean> {

    /** 测试流量标记请求头名称 */
    public static final String KEY = "X-Test-Flow";

    /** 单例实例 */
    public static final TestFlowContext CONTEXT = new TestFlowContext();

    /** 私有构造器，防止外部实例化 */
    private TestFlowContext() {}

    /**
     * 设置当前线程的测试流量标记
     *
     * @param testFlow 测试流量标记字符串，为 "true" 时标记为测试流量
     */
    public void set(String testFlow) {
        super.set(Boolean.TRUE.toString().equals(testFlow));
    }

    /**
     * 获取当前线程的测试流量标记
     *
     * @return true 表示测试流量，未设置时返回 false
     */
    @Override
    public Boolean get() {
        Boolean data = super.get();
        return ObjectUtil.isNotNullEmpty(data) && data;
    }

}
