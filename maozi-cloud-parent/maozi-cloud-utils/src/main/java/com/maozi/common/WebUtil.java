package com.maozi.common;

import cn.hutool.extra.servlet.JakartaServletUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Objects;

/**
 * Web 工具类
 * <p>
 * 提供 HTTP 请求/响应的常用操作方法，包括获取当前请求和响应对象、
 * 向响应中写入 JSON 数据、获取客户端真实 IP 地址等功能。
 * 主要用于在非 Controller 层（如 Service、Utils 等）中访问 Web 上下文信息。
 * </p>
 *
 * @author pengjinlong
 * @since 2026/4/24 14:11
 */
@Slf4j
public class WebUtil {

    /**
     * 本地IP地址
     */
    public final static String LOCAL_IP = "127.0.0.1";

    /**
     * 本地回环IP地址
     */
    public final static String LOCAL_LOOPBACK_IP = "0:0:0:0:0:0:0:1";

    /**
     * 判断当前线程是否处于 HTTP 请求上下文
     * <p>
     * 通过 {@link #getRequest()} 能否拿到当前 HttpServletRequest 来判断，
     * 用于在定时任务、消息消费者等非 Web 入口处跳过 Web 相关逻辑。
     * </p>
     *
     * @return 在 Web 请求线程中返回 {@code true}，否则返回 {@code false}
     */
    public static Boolean isHttpRequest() {
        return ObjectUtil.isNotNullEmpty(getRequest());
    }

    /**
     * 获取当前 HTTP 请求对象
     * <p>
     * 通过 Spring 的 RequestContextHolder 获取当前线程绑定的 Servlet 请求属性，
     * 进而获取 HttpServletRequest 对象。仅在 Web 请求线程中有效。
     * </p>
     *
     * @return 当前 HttpServletRequest 对象，如果不在 Web 请求上下文中则返回 null
     */
    public static HttpServletRequest getRequest() {
        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (ObjectUtil.isNullEmpty(servletRequestAttributes)) {
            return null;
        }
        return servletRequestAttributes.getRequest();
    }

    /**
     * 获取当前 HTTP 响应对象
     * <p>
     * 通过 Spring 的 RequestContextHolder 获取当前线程绑定的 Servlet 请求属性，
     * 进而获取 HttpServletResponse 对象。仅在 Web 请求线程中有效。
     * </p>
     *
     * @return 当前 HttpServletResponse 对象，如果不在 Web 请求上下文中则返回 null
     */
    public static HttpServletResponse getResponse() {
        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (ObjectUtil.isNullEmpty(servletRequestAttributes)) {
            return null;
        }
        return servletRequestAttributes.getResponse();
    }


    /**
     * 向 HTTP 响应中写入 JSON 格式的数据
     * <p>
     * 将指定的数据对象序列化为 JSON 并写入响应输出流，
     * 同时设置响应头 Content-Type 为 application/json。
     * 如果响应对象或数据为空，则不做任何操作；写出过程中发生异常时仅记录 error 日志，不向外抛出。
     * </p>
     *
     * @param response HTTP 响应对象
     * @param data 要写入的数据对象，将被序列化为 JSON 格式
     */
    public static void writeResponseBody(HttpServletResponse response,Object data){

        if(ObjectUtil.isNullEmpty(response) || ObjectUtil.isNullEmpty(data)){
            return;
        }

        response.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        try{JacksonUtil.getObjectMapper().writeValue(response.getOutputStream(),data);}catch (Exception e){
            log.error(e.getLocalizedMessage());
        }

    }

    /**
     * 获取当前请求的客户端真实 IP 地址
     * <p>
     * 无参重载，自动从 {@link #getRequest()} 取当前请求后委托给
     * Hutool 的 {@code JakartaServletUtil#getClientIP} 解析客户端 IP。
     * </p>
     *
     * @return 客户端真实 IP 地址字符串
     * @throws NullPointerException 不在 Web 请求上下文中调用时抛出
     */
    public static String getRequestHost() {
        return JakartaServletUtil.getClientIP(Objects.requireNonNull(getRequest()));
    }

    /**
     * 判断 IP 地址字符串是否为空、空白或 "unknown"
     * <p>
     * 用于在获取客户端 IP 时判断各个代理头字段是否有效。
     * </p>
     *
     * @param ip 待判断的 IP 字符串
     * @return 如果为 null、空白或 "unknown"（不区分大小写），返回 true
     */
    private static boolean isBlankOrUnknown(String ip) {
        return ObjectUtil.isNullEmpty(ip) || ip.isBlank() || "unknown".equalsIgnoreCase(ip);
    }

}
