package com.maozi.common;

import com.maozi.common.result.error.ErrorResult;
import com.maozi.common.result.error.code.ErrorCode;
import com.maozi.common.result.success.SuccessResult;

/**
 * 统一结果返回工具类
 * <p>
 * 封装成功和错误结果的构造方法，提供统一格式的 API 响应对象创建入口。
 * </p>
 *
 * @author pengjinlong
 * @since 2026/4/26 08:18
 */
public final class ResultUtil {

    /** 私有构造器，防止工具类被实例化 */
    private ResultUtil() {}

    /** 默认成功结果（无数据） */
    public static final SuccessResult<Void> DEFAULT_SUCCESS = new SuccessResult<>(null);

    /**
     * 返回默认成功结果（无数据）
     *
     * @return 成功结果
     */
    public static SuccessResult<Void> success() {
        return DEFAULT_SUCCESS;
    }

    /**
     * 返回带数据的成功结果
     *
     * @param data 返回数据
     * @param <T> 数据类型
     * @return 成功结果
     */
    public static <T> SuccessResult<T> success(T data) {
        return new SuccessResult<>(data);
    }

    /**
     * 返回带错误码的错误结果
     *
     * @param errorCode 错误码
     * @param <T> 数据类型
     * @return 错误结果
     */
    public static <T> ErrorResult<T> error(ErrorCode errorCode) {
        return new ErrorResult<>(errorCode);
    }

    /**
     * 返回带错误码和错误数据的错误结果
     *
     * @param errorCode 错误码
     * @param errorData 错误附加数据
     * @param <T> 数据类型
     * @return 错误结果
     */
    public static <T> ErrorResult<T> error(ErrorCode errorCode,T errorData) {
        return new ErrorResult<>(errorCode,errorData);
    }

}
