package com.maozi.common;

import org.slf4j.Logger;

import java.util.Map;

/**
 * 日志工具类
 * <p>
 * 封装统一的日志输出方法，支持 info/error 级别的字符串和 Map 格式日志，
 * 以及日志格式化。
 * </p>
 *
 * @author pengjinlong
 * @since 2026/4/25 01:37
 */
public final class LogUtil {

    /** 日志条目前缀 */
    private static final String ENTRY_PREFIX = "[ ";

    /** 键值分隔符 */
    private static final String KEY_VALUE_SEPARATOR = "：";

    /** 日志条目后缀 */
    private static final String ENTRY_SUFFIX = " ]  ";

    /** 条目前缀长度，用于拼接后删除首个条目的前缀 */
    private static final int ENTRY_PREFIX_LENGTH = ENTRY_PREFIX.length();

    /** 条目后缀长度，用于拼接后删除末尾条目的后缀 */
    private static final int ENTRY_SUFFIX_LENGTH = ENTRY_SUFFIX.length();

    /** 私有构造器，防止工具类被实例化 */
    private LogUtil() {}

    /**
     * 输出 info 级别日志
     *
     * @param log Logger 实例
     * @param message 日志内容
     */
    public static void info(Logger log, String message){
        log.info(message);
    }

    /**
     * 输出 info 级别日志（Map 格式）
     *
     * @param log Logger 实例
     * @param content 日志内容键值对
     */
    public static void info(Logger log, Map<String, String> content){
        log(log, false, content, null);
    }

    /**
     * 输出 error 级别日志
     *
     * @param log Logger 实例
     * @param message 日志内容
     */
    public static void error(Logger log, String message){
        log.error(message);
    }

    /**
     * 输出 error 级别日志（含异常堆栈）
     *
     * @param log Logger 实例
     * @param message 日志内容
     * @param e 异常对象
     */
    public static void error(Logger log, String message, Throwable e){
        log.error(message, e);
    }

    /**
     * 输出 error 级别日志（Map 格式）
     *
     * @param log Logger 实例
     * @param content 日志内容键值对
     */
    public static void error(Logger log, Map<String, String> content){
        log.error(LogUtil.convertLog(content));
    }

    /**
     * 输出 error 级别日志（Map 格式，含异常堆栈）
     *
     * @param log Logger 实例
     * @param content 日志内容键值对
     * @param e 异常对象
     */
    public static void error(Logger log, Map<String, String> content, Throwable e){
        log.error(LogUtil.convertLog(content), e);
    }

    /**
     * 根据 error 标志输出日志（Map 格式）
     *
     * @param log Logger 实例
     * @param isException 是否为错误日志
     * @param content 日志内容键值对
     * @param e 异常对象（isException 为 true 时随日志输出堆栈）
     */
    public static void log(Logger log, boolean isException, Map<String, String> content, Throwable e) {
        String message = LogUtil.convertLog(content);
        if (isException) {
            log.error(message, e);
        } else {
            log.info(message);
        }
    }

    /**
     * 将 Map 格式的日志内容转换为字符串
     * <p>
     * 条目格式为 {@code key：value}，多条目间以 {@code " ]  [ "} 分隔，
     * 如两条目时输出 {@code key1：value1 ]  [ key2：value2}。
     * </p>
     *
     * @param content 日志内容键值对
     * @return 格式化后的日志字符串，入参为空时返回 {@code null}
     */
    public static String convertLog(Map<String, String> content) {

        if (CollectionUtil.isEmpty(content)){
            return null;
        }

        StringBuilder sb = new StringBuilder();

        for (Map.Entry<String, String> entry : content.entrySet()) {
            sb.append(ENTRY_PREFIX).append(entry.getKey()).append(KEY_VALUE_SEPARATOR)
              .append(entry.getValue()).append(ENTRY_SUFFIX);
        }
        sb.delete(0, ENTRY_PREFIX_LENGTH);
        int length = sb.length();
        sb.delete(length - ENTRY_SUFFIX_LENGTH, length);

        return sb.toString();

    }

}
