package com.maozi.common.context;

/**
 * SQL 日志上下文
 * <p>
 * 存储当前线程的 SQL 日志构建器，用于在同一线程中累积 SQL 日志信息
 * （由 MyBatis 日志实现写入，各入口日志切面在请求结束时读取）。
 * </p>
 *
 * @author maozi
 * @date 2026/11/13
 */
public class SqlLogContext extends AbstractRequestContextHolder<StringBuilder> {

    /** 单例实例 */
    public static final SqlLogContext CONTEXT = new SqlLogContext();

    /** 私有构造器，防止外部实例化 */
    private SqlLogContext() {}

}
