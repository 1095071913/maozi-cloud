package com.maozi.mvc.config.code;

import cn.hutool.extra.spring.SpringUtil;
import com.maozi.common.LogUtil;
import com.maozi.common.result.error.code.BaseErrorCode;
import com.maozi.common.result.error.code.ErrorCode;
import com.maozi.common.result.error.code.SystemErrorCode;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 错误码配置
 * <p>
 * 启动时扫描 Spring 容器中所有 {@link BaseErrorCode} 的实现类，
 * 通过反射提取其中的 {@link ErrorCode} 字段，汇总到全局错误码映射表中。
 * 提供根据错误码值查找错误信息的统一入口。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Data
@Slf4j
@Component
public class CodeConfig {

    /**
     * 全局错误码映射表，键为错误码值，值为错误码对象
     * （仅类内部可写，外部通过 {@link #getErrorCodes()} 只读视图
     * 或 {@link #getErrorCode(Integer)} 访问；启动线程写入后供任意请求线程并发读，
     * 使用 ConcurrentHashMap 保证跨线程安全发布）
     */
    private static final Map<Integer, ErrorCode> ERROR_CODES = new ConcurrentHashMap<>(256);

    /**
     * 初始化错误码
     * <p>
     * 从 Spring 容器中获取所有 {@link BaseErrorCode} 类型的 Bean，
     * 通过反射遍历其字段，提取 {@link ErrorCode} 类型的字段值并注册到全局映射表。
     * </p>
     */
    @PostConstruct
    private void initErrorCode() {

        // 从 Spring 容器中获取所有 BaseErrorCode 类型的 Bean
        Map<String, BaseErrorCode> codeBeans = SpringUtil.getBeansOfType(BaseErrorCode.class);

        for (BaseErrorCode errorCodeBean : codeBeans.values()) {
            registerErrorCode(errorCodeBean);
        }

    }

    /**
     * 注册单个错误码 Bean 中的 ErrorCode 字段
     * <p>
     * 通过反射遍历指定 Bean 的所有声明字段，提取 {@link ErrorCode} 类型的字段值，
     * 以错误码值为键注册到全局映射表；单个 Bean 反射失败仅记录日志，
     * 不影响其余 Bean 注册。
     * </p>
     *
     * @param errorCodeBean 错误码 Bean
     */
    private void registerErrorCode(BaseErrorCode errorCodeBean) {

        try {

            // 通过反射遍历 Bean 的所有声明字段
            for (Field field : errorCodeBean.getClass().getDeclaredFields()) {

                // 设置可访问私有字段
                field.setAccessible(true);

                // 仅提取 ErrorCode 类型的字段值
                if (field.get(errorCodeBean) instanceof ErrorCode errorCode) {
                    // 以错误码值为键注册到全局映射表
                    ERROR_CODES.put(errorCode.getCode(), errorCode);
                }

            }

        } catch (Exception e) {
            // 仅捕获 Exception，OutOfMemoryError 等 Error 交由 JVM 处理，避免掩盖启动期致命问题
            LogUtil.error(log, "注册错误错误码失败", e);
        }

    }

    /**
     * 根据错误码值获取错误码对象
     *
     * @param code 错误码值
     * @return 对应的错误码对象，不存在时返回 {@link SystemErrorCode#NOT_EXIST_CODE_ERROR}
     */
    public static ErrorCode getErrorCode(Integer code) {

        ErrorCode errorCode = ERROR_CODES.get(code);

        return Objects.isNull(errorCode) ? SystemErrorCode.NOT_EXIST_CODE_ERROR : errorCode;

    }

    /**
     * 获取全局错误码映射表的只读视图
     *
     * @return 不可变的错误码映射表视图
     */
    public static Map<Integer, ErrorCode> getErrorCodes() {
        return Collections.unmodifiableMap(ERROR_CODES);
    }

}
