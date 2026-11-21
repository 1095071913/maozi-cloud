package com.maozi.system.redirect.util;

import com.maozi.common.ObjectUtil;
import com.maozi.common.WebUtil;
import com.maozi.system.redirect.enums.RedirectTypeEnum;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 重定向指示工具类
 * <p>
 * 通过设置 {@code X-Redirect} 响应头告知前端需要重定向的目标页面类型
 * （如会话失效后跳转登录页），由前端根据该响应头执行跳转。
 * </p>
 *
 * @author pengjinlong
 * @date 2026/8/14
 */
public class RedirectUtil {

    /** 重定向指示响应头名称 */
    public static final String REDIRECT_KEY = "X-Redirect";

    /**
     * 设置重定向指示响应头
     * <p>
     * 将重定向类型的值写入 {@code X-Redirect} 响应头；当前线程无响应上下文时不做处理。
     * </p>
     *
     * @param redirectType 重定向类型
     */
    public static void redirect(RedirectTypeEnum redirectType){
        HttpServletResponse response = WebUtil.getResponse();
        if (ObjectUtil.isNotNull(response)){
            response.setHeader(REDIRECT_KEY, redirectType.getValue().toString());
        }
    }

}
