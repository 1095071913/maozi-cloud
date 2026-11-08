package com.maozi.common;

import org.slf4j.Logger;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

/**
 * 日志工具类
 * <p>
 * 封装统一的日志输出方法，支持 info/error 级别的字符串和 Map 格式日志，
 * 以及 SQL 日志的线程本地存储和日志格式化。
 * </p>
 *
 * @author pengjinlong
 * @since 2026/4/25 01:37
 */
public class LogUtil {

    /** SQL 日志线程变量，用于在同一线程中累积 SQL 日志信息 */
    public static final ThreadLocal<StringBuilder> sqlLog = new ThreadLocal<>();

    /**
     * 输出 info 级别日志
     *
     * @param log Logger 实例
     * @param message 日志内容
     */
    public static void info(Logger log, String message){
        log(log,false,message);
    }

    /**
     * 输出 info 级别日志（Map 格式）
     *
     * @param log Logger 实例
     * @param content 日志内容键值对
     */
    public static void info(Logger log,Map<String, String> content){
        log(log,false,content);
    }

    /**
     * 输出 error 级别日志
     *
     * @param log Logger 实例
     * @param message 日志内容
     */
    public static void error(Logger log, String message){
        log(log,true,message);
    }

    /**
     * 输出 error 级别日志（Map 格式）
     *
     * @param log Logger 实例
     * @param content 日志内容键值对
     */
    public static void error(Logger log, Map<String, String> content){
        log(log,true,content);
    }

    /**
     * 输出 error 级别异常堆栈日志
     *
     * @param log Logger 实例
     * @param e 异常对象
     */
    public static void error(Logger log,Throwable e){
        log.error("",e);
    }

    /**
     * 根据 error 标志输出日志
     *
     * @param log Logger 实例
     * @param error 是否为错误日志
     * @param message 日志内容
     */
    public static void log(Logger log, boolean error, String message) {
        if (error) {
            log.error(message);
        } else {
            log.info(message);
        }
    }

    /**
     * 根据 error 标志输出日志（Map 格式）
     *
     * @param log Logger 实例
     * @param error 是否为错误日志
     * @param content 日志内容键值对
     */
    public static void log(Logger log, boolean error, Map<String, String> content) {
        String message = LogUtil.convertLog(content);
        log(log, error, message);
    }

    /**
     * 将 Map 格式的日志内容转换为字符串
     * <p>
     * 每个条目按 {@code [ key：value ]  } 拼接，并去除整串最前面的 {@code "[ "} 与最后面的 {@code " ]  "}。
     * 例如两条目时实际输出为：{@code key1：value1 ]  [ key2：value2}
     * </p>
     *
     * @param content 日志内容键值对
     * @return 格式化后的日志字符串，入参为空时返回 {@code null}
     */
    public static String convertLog(Map<String, String> content) {

        if(ObjectUtil.isNullEmpty(content)){
            return null;
        }

        StringBuilder sb = new StringBuilder();

        for (Map.Entry<String, String> entry : content.entrySet()) {
            sb.append("[ ").append(entry.getKey()).append("：").append(entry.getValue()).append(" ]  ");
        }
        sb.delete(0, 2);
        sb.delete(sb.length() - 4, sb.length());

        return sb.toString();

    }

    /**
     * 获取异常的堆栈跟踪信息
     *
     * @param t 异常对象
     * @return 堆栈跟踪字符串
     */
    public static String getStackTraceLog(Throwable t) {

        StringWriter sw = new StringWriter();

        PrintWriter pw = new PrintWriter(sw);

        t.printStackTrace(pw);

        return sw.toString();

    }

}
