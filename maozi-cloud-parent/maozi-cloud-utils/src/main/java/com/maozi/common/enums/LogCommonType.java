package com.maozi.common.enums;

import lombok.Getter;

/**
 * 日志通用类型枚举
 * <p>
 * 标识日志来源入口类型（网关、Web 接口、RPC、定时任务、MQ 等），便于日志分类与排查。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
public enum LogCommonType implements BaseEnum {

    /** 网关层日志 */
    GATEWAY(0,"Gateway"),

    /** Web 接口日志 */
    WEB(1,"Web"),

    /** RPC 调用日志 */
    RPC(2,"RPC"),

    /** 定时任务日志 */
    JOB(3,"Job"),

    /** 消息队列日志 */
    MQ(4,"MQ"),

    /** RestTemplate 三方调用日志 */
    REST_TEMPLATE(5,"RestTemplate"),

    /** WebSocket 日志 */
    WEB_SOCKET(6,"WebSocket"),

    /** RPC 重试日志 */
    RPC_RETRY(7,"RPCRetry"),

    ;

    /**
     * 按整型值和类型描述构造枚举实例
     *
     * @param value 枚举整型值
     * @param desc 日志类型英文描述
     */
    LogCommonType(Integer value,String desc) {

        this.value = value;

        this.desc = desc;

    }

    /** 枚举的整型值 */
    @Getter
    private final Integer value;

    /** 日志类型的英文描述 */
    @Getter
    private final String desc;

    /** 返回 {@code value.desc} 格式的枚举描述 */
    @Override
    public String toString() {
        return value + "." + desc;
    }

}
