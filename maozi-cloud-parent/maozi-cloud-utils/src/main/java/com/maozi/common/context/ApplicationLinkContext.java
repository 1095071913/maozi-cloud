package com.maozi.common.context;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.maozi.common.JacksonUtil;
import com.maozi.common.LogUtil;
import com.maozi.common.ObjectUtil;
import com.maozi.common.dto.CurrentUserInfo;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;

import java.util.function.Function;

/**
 * 应用链路上下文
 * <p>
 * 使用 TransmittableThreadLocal 存储当前请求的链路信息，
 * 包括版本号、测试流量标记、当前登录用户信息（{@link CurrentUserInfo}）
 * 以及链路追踪 ID（traceId，并同步写入 MDC 便于日志关联），
 * 支持线程池和异步场景下的上下文传递。
 * 同时提供基于 Function 的用户属性提取和上下文清理功能。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Data
public class ApplicationLinkContext {

    /** 版本号请求头名称 */
    public static final String VERSION_KEY = "X-Version";

    /** 当前登录用户信息请求头名称 */
    public static final String CURRENT_USER_INFO_KEY = "X-CurrentUserInfo";

    /** 测试流量标记请求头名称 */
    public static final String TEST_FLOW_KEY = "X-Test-Flow";

    /** 链路追踪 ID 请求头名称 */
    public static final String TRACE_ID_KEY = "X-TraceId";

    /** 链路追踪 ID 未初始化时的占位值（32 个 0） */
    public static final String TRACE_ID_VALUE = "00000000000000000000000000000000";

    /** Nacos 元数据中的版本键 */
    public static final String NACOS_VERSION_KEY = "version";

    /** 默认应用版本（主版本） */
    public static final String APPLICATION_DEFAULT_VERSION = "main";

    /** MDC 中存放链路追踪 ID 的键名 */
    public static final String MDC_TRACE_ID_KEY = "trace_id";

    /** 当前线程的链路请求ID */
    private static final TransmittableThreadLocal<String> traceIds = new TransmittableThreadLocal<>();

    /** 当前线程的版本号（用于灰度路由） */
    private static final TransmittableThreadLocal<String> versions = new TransmittableThreadLocal<>();

    /** 当前线程的测试流量标记（true 表示测试流量） */
    private static final TransmittableThreadLocal<Boolean> testFlows = new TransmittableThreadLocal<>();

    /** 当前线程的登录用户信息（含用户 ID、客户端 ID、用户名） */
    private static final TransmittableThreadLocal<CurrentUserInfo> currentUserInfos = new TransmittableThreadLocal<>();

    /**
     * 获取当前线程的链路追踪 ID
     *
     * @return 链路追踪 ID，未设置时返回 null
     */
    public static String getTraceId() {
        return traceIds.get();
    }

    /**
     * 获取当前线程的版本号
     *
     * @return 版本号，未设置时返回 null
     */
    public static String getVersion() {
        return versions.get();
    }

    /**
     * 获取当前线程的测试流量标记
     *
     * @return 是否测试流量
     */
    public static boolean getTestFlow() {
        return testFlows.get();
    }

    /**
     * 获取当前线程的登录用户信息
     *
     * @return 当前登录用户信息，未登录时返回 null
     */
    public static CurrentUserInfo getCurrentUserInfo() {
        return currentUserInfos.get();
    }

    /**
     * 通过函数式接口提取当前登录用户的指定属性
     *
     * @param function 属性提取函数（如 {@code CurrentUserInfo::getUserId}）
     * @param <R> 目标属性类型
     * @return 目标属性值，未登录时返回 null
     */
    public static <R> R getCurrentUserInfo(Function<CurrentUserInfo, R> function) {
        CurrentUserInfo currentUserInfo = currentUserInfos.get();
        if(ObjectUtil.isNullEmpty(currentUserInfo)){
            return null;
        }
        return function.apply(currentUserInfo);
    }

    /**
     * 获取版本号，为空时返回默认版本
     *
     * @param version 版本号对象
     * @return 版本号字符串
     */
    public static String getVersionDefault(Object version){
        boolean hasVersion = ObjectUtil.isNotNullEmpty(version)
                && StringUtils.isNotBlank(version.toString());
        return hasVersion ? version.toString() : ApplicationLinkContext.APPLICATION_DEFAULT_VERSION;
    }

    /**
     * 设置当前线程的链路追踪 ID，非空时同时写入 MDC 便于日志关联
     *
     * @param traceId 链路追踪 ID，为空时不做任何处理
     */
    public static void setTraceId(String traceId){
        if(ObjectUtil.isNotNullEmpty(traceId)){
            traceIds.set(traceId);
            MDC.put(MDC_TRACE_ID_KEY,traceId);
        }
    }

    /**
     * 设置当前线程的版本号
     *
     * @param version 版本号
     */
    public static void setVersion(String version) {
        versions.set(version);
    }

    /**
     * 设置当前线程的测试流量标记
     *
     * @param testFlow 测试流量标记字符串，为 "true" 时标记为测试流量
     */
    public static void setTestFlow(String testFlow) {
        testFlows.set(Boolean.TRUE.toString().equals(testFlow));
    }

    /**
     * 设置当前线程的登录用户信息
     *
     * @param currentUserInfo 当前登录用户信息
     */
    public static void setCurrentUserInfo(CurrentUserInfo currentUserInfo) {
        currentUserInfos.set(currentUserInfo);
    }

    /**
     * 以 JSON 字符串形式设置当前线程的登录用户信息
     *
     * @param currentUserInfoJson 用户信息的 JSON 字符串，为空时不做任何处理
     */
    public static void setCurrentUserInfo(String currentUserInfoJson){
        if(ObjectUtil.isNotNullEmpty(currentUserInfoJson)){
            ApplicationLinkContext.currentUserInfos.set(
                    JacksonUtil.jsonToObject(currentUserInfoJson, CurrentUserInfo.class));
        }
    }

    /**
     * 清理当前线程的链路上下文（含 SQL 日志、版本号、测试流量标记、用户信息、链路追踪 ID 及 MDC）
     */
    public static void clearContext(){
        LogUtil.SQL_LOG.remove();
        versions.remove();
        traceIds.remove();
        testFlows.remove();
        currentUserInfos.remove();
        MDC.clear();
    }

}
