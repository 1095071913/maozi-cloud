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

    /** 本地 IP 地址 */
    public final static String LOCAL_IP = "127.0.0.1";

    /** 本地回环 IP 地址（IPv6） */
    public final static String LOCAL_LOOPBACK_IP = "0:0:0:0:0:0:0:1";

    /**
     * 判断当前线程是否处于 HTTP 请求上下文
     *
     * @return 在 Web 请求线程中返回 true，否则返回 false
     */
    public static Boolean isHttpRequest() {
        return ObjectUtil.isNotNullEmpty(getRequest());
    }

    /**
     * 获取当前 HTTP 请求对象
     *
     * @return 当前 HttpServletRequest，不在 Web 请求上下文中时返回 null
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
     *
     * @return 当前 HttpServletResponse，不在 Web 请求上下文中时返回 null
     */
    public static HttpServletResponse getResponse() {
        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (ObjectUtil.isNullEmpty(servletRequestAttributes)) {
            return null;
        }
        return servletRequestAttributes.getResponse();
    }


    /**
     * 将数据序列化为 JSON 写入响应输出流（Content-Type 为 application/json）
     * <p>
     * 响应对象或数据为空时不做任何操作；写出异常时仅记录 error 日志，不向外抛出。
     * </p>
     *
     * @param response HTTP 响应对象
     * @param data 要写入的数据对象
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
     *
     * @return 客户端 IP 地址
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
