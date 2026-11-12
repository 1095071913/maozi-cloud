package com.maozi.common.constant;

/**
 * 日志标签常量类
 * <p>
 * 定义统一的日志字段标签名称，用于结构化日志输出。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
public class LogTag {

    /** 链路追踪 ID */
    public static final String TID = "Tid";

    /** Nacos 配置信息 */
    public static final String NACOS = "Nacos";

    /** 初始化耗时 */
    public static final String INIT_TIME = "InitTime";

    /** 日志类型 */
    public static final String TYPE = "Type";

    /** 服务名称 */
    public static final String SERVICE_NAME = "ServiceName";

    /** 服务端口 */
    public static final String SERVICE_PORT = "ServicePort";

    /** Dubbo 端口 */
    public static final String DUBBO_PORT = "DubboPort";

    /** IP 地址 */
    public static final String IP = "IP";

    /** HTTP 方法 */
    public static final String METHOD = "Method";

    /** 请求 URL */
    public static final String URL = "URL";

    /** 响应状态码 */
    public static final String CODE = "Code";

    /** 调用函数 */
    public static final String FUNCTION = "Function";

    /** 请求参数 */
    public static final String PARAM = "Param";

    /** 响应数据 */
    public static final String DATA = "Data";

    /** SQL 语句 */
    public static final String SQL = "SQL";

    /** 错误用户 */
    public static final String ERROR_USER = "ErrorUser";

    /** 错误描述 */
    public static final String ERROR_DESC = "ErrorDesc";

    /** 错误行号 */
    public static final String ERROR_LINE = "ErrorLine";

    /** 响应耗时 */
    public static final String RT = "RT";

}
