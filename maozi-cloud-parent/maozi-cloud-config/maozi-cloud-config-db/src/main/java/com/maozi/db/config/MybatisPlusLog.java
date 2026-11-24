package com.maozi.db.config;

import com.maozi.common.constant.SymbolConstant;
import com.maozi.common.context.SqlLogContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.logging.Log;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * MyBatis-Plus 自定义日志实现
 * <p>
 * 拦截 MyBatis-Plus 的 SQL 日志输出，将 SQL 语句和参数收集到线程变量中，
 * 用于在请求结束时统一记录完整的 SQL 日志。仅当日志内容包含 SQL 准备语句
 * （==&gt;  Preparing:）或参数信息（==&gt; Parameters:）时才进行收集。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Slf4j
public class MybatisPlusLog implements Log {

    /** MyBatis SQL 准备语句日志前缀 */
    private static final String PREPARING_PREFIX = "==>  Preparing: ";

    /** MyBatis SQL 参数日志前缀 */
    private static final String PARAMETERS_PREFIX = "==> Parameters: ";

    /** SQL 参数拆分正则（右括号 + 逗号），预编译避免重复编译开销 */
    private static final Pattern PARAM_SPLIT_PATTERN = Pattern.compile("\\),");

    /**
     * 构造方法
     *
     * @param clazz 类名（MyBatis 内部传入）
     */
    public MybatisPlusLog(String clazz) {}

    /**
     * 是否启用 debug 级别
     * <p>
     * 返回 true 使 MyBatis 将 SQL 日志交给 {@link #debug(String)} 收集
     * </p>
     *
     * @return 始终返回 true
     */
    @Override
    public boolean isDebugEnabled() {
        return true;
    }

    /**
     * 是否启用 trace 级别
     *
     * @return 始终返回 true
     */
    @Override
    public boolean isTraceEnabled() {
        return true;
    }

    /**
     * 记录 error 日志（含异常堆栈）
     *
     * @param s 日志内容
     * @param e 异常对象
     */
    @Override
    public void error(String s, Throwable e) {
        log.error(s, e);
    }

    /**
     * 记录 error 日志
     *
     * @param s 日志内容
     */
    @Override
    public void error(String s) {
        log.error(s);
    }

    /**
     * 收集 SQL 日志
     * <p>
     * 将 SQL 的 Preparing 和 Parameters 信息收集到线程变量中，
     * 并将参数值替换到 SQL 的占位符中，形成完整的可执行 SQL。
     * </p>
     *
     * @param s SQL 日志内容
     */
    @Override
    public void debug(String s) {

        // 从线程变量中获取 SQL 日志构建器
        StringBuilder sqlLog = SqlLogContext.CONTEXT.get();

        // 如果当前线程还没有初始化 SQL 日志构建器，则创建并设置到线程变量中
        if (Objects.isNull(sqlLog)) {
            sqlLog = new StringBuilder();
            SqlLogContext.CONTEXT.set(sqlLog);
        }

        // 处理 SQL 准备语句：提取 SQL 模板（包含 ? 占位符）
        if (s.contains(PREPARING_PREFIX)) {
            s = s.replace(PREPARING_PREFIX, SymbolConstant.EMPTY);
            sqlLog.append(s);
        }

        // 处理 SQL 参数：将参数值替换到 SQL 模板中的 ? 占位符位置
        if (s.contains(PARAMETERS_PREFIX)) {

            s = s.replace(PARAMETERS_PREFIX, SymbolConstant.EMPTY);

            if (StringUtils.isNotEmpty(s)) {

                // 参数格式为 "value(Type),value(Type),..." 按 "),"(右括号+逗号) 拆分
                String[] params = PARAM_SPLIT_PATTERN.split(s);

                for (String param : params) {

                    // 提取参数值部分（"(" 之前的字符串），并加上单引号包裹
                    param = SymbolConstant.SINGLE_QUOTE
                            + param.substring(0, param.indexOf(SymbolConstant.LEFT_PARENTHESIS))
                            + SymbolConstant.SINGLE_QUOTE;

                    // 在 SQL 模板中找到第一个 "?" 占位符，用参数值替换
                    int index = sqlLog.indexOf(SymbolConstant.QUESTION_MARK);
                    if (index != -1) {
                        sqlLog.replace(index, index + 1, param);

                    }

                }

            }

            // 一条完整的 SQL 拼接完成后，追加分号分隔符
            sqlLog.append(SymbolConstant.SEMICOLON);

        }

    }

    /**
     * trace 日志（空实现）
     *
     * @param s 日志内容
     */
    @Override
    public void trace(String s) {}

    /**
     * warn 日志（空实现）
     *
     * @param s 日志内容
     */
    @Override
    public void warn(String s) {}

}
