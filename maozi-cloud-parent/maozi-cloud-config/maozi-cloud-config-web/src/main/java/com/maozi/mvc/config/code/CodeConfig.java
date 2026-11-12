package com.maozi.mvc.config.code;

import cn.hutool.extra.spring.SpringUtil;
import com.maozi.common.LogUtil;
import com.maozi.common.ObjectUtil;
import com.maozi.common.result.error.code.AbstractBaseErrorCode;
import com.maozi.common.result.error.code.ErrorCode;
import com.maozi.common.result.error.code.SystemErrorCode;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 错误码配置
 * <p>
 * 启动时扫描 Spring 容器中所有 {@link AbstractBaseErrorCode} 的实现类，
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

    /** 全局错误码映射表，键为错误码值，值为错误码对象（仅类内部可写，外部通过 {@link #getErrorCodes()} 只读视图或 {@link #getErrorCode(Integer)} 访问） */
    private static final Map<Integer, ErrorCode> ERROR_CODES = new HashMap<>();

    /**
     * 初始化错误码
     * <p>
     * 从 Spring 容器中获取所有 {@link AbstractBaseErrorCode} 类型的 Bean，
     * 通过反射遍历其字段，提取 {@link ErrorCode} 类型的字段值并注册到全局映射表。
     * </p>
     */
    @PostConstruct
    private void initErrorCode(){

        // 从 Spring 容器中获取所有 AbstractBaseErrorCode 类型的 Bean
        Map<String, AbstractBaseErrorCode> codeBeans = SpringUtil.getBeansOfType(AbstractBaseErrorCode.class);

        for(AbstractBaseErrorCode errorCodeBean : codeBeans.values()){

            try {

                // 通过反射遍历 Bean 的所有声明字段
                for (Field field : errorCodeBean.getClass().getDeclaredFields()) {

                    field.setAccessible(true); // 设置可访问私有字段

                    // 仅提取 ErrorCode 类型的字段值
                    if(field.get(errorCodeBean) instanceof ErrorCode errorCode){
                        // 以错误码值为键注册到全局映射表
                        ERROR_CODES.put(errorCode.getCode(),errorCode);
                    }

                }

            } catch (Exception e) {
                LogUtil.error(log,e);
            }

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

        return ObjectUtil.isNullEmpty(errorCode) ? SystemErrorCode.NOT_EXIST_CODE_ERROR : errorCode;

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
