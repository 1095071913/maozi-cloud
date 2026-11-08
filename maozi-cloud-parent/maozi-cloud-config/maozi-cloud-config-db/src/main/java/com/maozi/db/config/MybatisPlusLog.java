package com.maozi.db.config;

import com.maozi.common.LogUtil;
import com.maozi.common.ObjectUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.logging.Log;

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

    /**
     * 构造方法
     *
     * @param clazz 类名（MyBatis 内部传入）
     */
    public MybatisPlusLog(String clazz) {}

    /**
     * 是否启用 debug 级别
     *
     * @return 始终返回 true
     */
    public boolean isDebugEnabled() {return true;}

    /**
     * 是否启用 trace 级别
     *
     * @return 始终返回 true
     */
    public boolean isTraceEnabled() {return true;}

    /**
     * 记录 error 日志（当前实现未输出异常堆栈，仅记录日志内容）
     *
     * @param s 日志内容
     * @param e 异常对象（未被使用，不会随日志输出）
     */
    public void error(String s, Throwable e) {log.error(s);}

    /**
     * 记录 error 日志
     *
     * @param s 日志内容
     */
    public void error(String s) {log.error(s); }

    /**
     * 收集 SQL 日志
     * <p>
     * 将 SQL 的 Preparing 和 Parameters 信息收集到线程变量中，
     * 并将参数值替换到 SQL 的占位符中，形成完整的可执行 SQL。
     * </p>
     *
     * @param s SQL 日志内容
     */
    public void debug(String s) {

        // 从线程变量中获取 SQL 日志构建器
        StringBuilder sqlLog = LogUtil.sqlLog.get();

        // 如果当前线程还没有初始化 SQL 日志构建器，则创建并设置到线程变量中
        if(ObjectUtil.isNullEmpty(sqlLog)) {

            sqlLog = new StringBuilder();

            LogUtil.sqlLog.set(sqlLog);

        }

        // 处理 SQL 准备语句：提取 SQL 模板（包含 ? 占位符）
        if(s.contains("==>  Preparing: ")) {

            // 去掉前缀，仅保留纯 SQL 语句
            s=s.replace("==>  Preparing: ","");

            sqlLog.append(s);

        }

        // 处理 SQL 参数：将参数值替换到 SQL 模板中的 ? 占位符位置
        if(s.contains("==> Parameters: ")) {

            // 去掉前缀，仅保留参数列表
            s=s.replace("==> Parameters: ","");

            if(StringUtils.isNotBlank(s)) {

                // 参数格式为 "value(Type),value(Type),..." 按 "),"(右括号+逗号) 拆分
                String [] params = s.split("\\),");

                for (String param : params) {

                    // 提取参数值部分（"(" 之前的字符串），并加上单引号包裹
                    param = "'" + param.substring(0, param.indexOf("(")) + "'";

                    // 在 SQL 模板中找到第一个 "?" 占位符，用参数值替换
                    int index = sqlLog.indexOf("?");

                    if (index != -1) {
                        sqlLog.replace(index, index + 1, param);

                    }

                }

            }

            // 一条完整的 SQL 拼接完成后，追加分号分隔符
            sqlLog.append(";");

        }

    }

    /**
     * trace 日志（空实现）
     *
     * @param s 日志内容
     */
    public void trace(String s) {}

    /**
     * warn 日志（空实现）
     *
     * @param s 日志内容
     */
    public void warn(String s) {}

}
