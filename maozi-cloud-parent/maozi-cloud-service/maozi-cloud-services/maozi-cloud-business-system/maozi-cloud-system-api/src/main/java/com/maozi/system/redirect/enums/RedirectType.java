package com.maozi.system.redirect.enums;

import com.maozi.common.enums.BaseEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/**
 * 重定向类型枚举
 * <p>
 * 定义前端重定向目标的类型，目前支持登录页；
 * 枚举值通过 {@code X-Redirect} 响应头传递给前端，由前端执行跳转。
 * </p>
 *
 * @author pengjinlong
 * @since 2026/8/14 13:06
 */
@Schema(description = "重定向类型",type = "integer")
public enum RedirectType implements BaseEnum {

    /** 重定向到登陆页 */
    LOGIN(0,"登陆页"),

    ;

    /**
     * 枚举构造方法
     *
     * @param value 枚举的整型值，写入 X-Redirect 响应头的值
     * @param desc  枚举的中文描述，用于展示和日志输出
     */
    RedirectType(Integer value,String desc) {

        this.value = value;

        this.desc = desc;

    }

    /** 枚举的整型值，通过 X-Redirect 响应头传递给前端（不落库存储） */
    @Getter
    private final Integer value;

    /** 枚举的中文描述，用于展示和日志输出 */
    @Getter
    private final String desc;

    /**
     * 输出枚举的字符串表示
     *
     * @return 格式为 "值.描述" 的字符串，例如 "0.登陆页"
     */
    @Override
    public String toString() {
        return value + "." + desc;
    }

}
