package com.maozi.common.context;

import com.maozi.common.JacksonUtil;
import com.maozi.common.ObjectUtil;
import com.maozi.common.dto.CurrentUserInfo;

import java.util.function.Function;

/**
 * 当前登录用户信息上下文
 * <p>
 * 存储当前线程的登录用户信息（{@link CurrentUserInfo}，含用户 ID、客户端 ID、用户名），
 * 支持从请求头 JSON 字符串反序列化设置，并提供基于 Function 的属性提取。
 * </p>
 *
 * @author maozi
 * @date 2026/11/13
 */
public class CurrentUserInfoContext extends AbstractRequestContextHolder<CurrentUserInfo> {

    /** 当前登录用户信息请求头名称 */
    public static final String KEY = "X-CurrentUserInfo";

    /** 单例实例 */
    public static final CurrentUserInfoContext CONTEXT = new CurrentUserInfoContext();

    /** 私有构造器，防止外部实例化 */
    private CurrentUserInfoContext() {}

    /**
     * 以 JSON 字符串形式设置当前线程的登录用户信息
     *
     * @param currentUserInfoJson 用户信息的 JSON 字符串，为空时不做任何处理
     */
    public void set(String currentUserInfoJson) {
        if (ObjectUtil.isNotNullEmpty(currentUserInfoJson)) {
            super.set(JacksonUtil.jsonToObject(currentUserInfoJson, CurrentUserInfo.class));
        }
    }

    /**
     * 通过函数式接口提取当前登录用户的指定属性
     *
     * @param function 属性提取函数（如 {@code CurrentUserInfo::getUserId}）
     * @param <R> 目标属性类型
     * @return 目标属性值，未登录时返回 null
     */
    public <R> R get(Function<CurrentUserInfo, R> function) {
        CurrentUserInfo currentUserInfo = super.get();
        if (ObjectUtil.isNullEmpty(currentUserInfo)) {
            return null;
        }
        return function.apply(currentUserInfo);
    }

    /**
     * 获取当前登录用户信息的 JSON 字符串（用于服务间透传）
     *
     * @return 用户信息 JSON 字符串，未登录时返回字面量 "null"，序列化失败时返回 null
     */
    public String getJson() {
        return JacksonUtil.objectToJson(super.get());
    }

}
