package com.maozi.common;

import com.maozi.common.result.error.code.ErrorCode;
import com.maozi.common.result.error.code.SystemErrorCode;
import com.maozi.common.result.error.exception.BusinessResultException;

import java.util.Objects;

/**
 * 对象工具类
 * <p>
 * 提供对象空值判断（null 判断）、相等比较、条件校验等通用方法。
 * </p>
 *
 * @author pengjinlong
 * @date 2026/4/29
 */
public final class ObjectUtil {

    /** 私有构造器，防止工具类被实例化 */
    private ObjectUtil() {}

    /**
     * 判断两个对象是否相等
     *
     * @param aParam 对象 aParam
     * @param bParam 对象 bParam
     * @return 相等返回 true
     */
    public static Boolean equals(Object aParam, Object bParam) {
        return Objects.equals(aParam, bParam);
    }

    /**
     * 判断对象是否不为空
     *
     * @param data 待检查对象
     * @return 不为空返回 true
     */
    public static boolean isNotNull(Object data) {
        return !isNull(data);
    }

    /**
     * 判断对象是否为 null
     *
     * @param data 待检查对象
     * @return 为 null 返回 true
     */
    public static boolean isNull(Object data) {
        return data == null;
    }

    /**
     * 对象为空时抛出业务异常（默认数据不存在错误码）
     *
     * @param data 待检查对象
     * @param sourceName 资源名称
     * @throws BusinessResultException 对象为 null 时抛出
     */
    public static void isNullThrowError(Object data, String sourceName) {
        isNullThrowError(data, SystemErrorCode.DATA_NOT_EXIST_ERROR, sourceName);
    }

    /**
     * 对象不为空时抛出业务异常（默认数据已存在错误码）
     *
     * @param data 待检查对象
     * @param sourceName 资源名称
     * @throws BusinessResultException 对象不为空时抛出
     */
    public static void isNotNullThrowError(Object data, String sourceName) {
        isNotNullThrowError(data, SystemErrorCode.DATA_EXIST_ERROR, sourceName);
    }

    /**
     * 对象为空时抛出业务异常
     *
     * @param data 待检查对象
     * @param errorCode 错误码
     * @param resourceName 资源名称
     * @throws BusinessResultException 对象为 null 时抛出
     */
    public static void isNullThrowError(Object data, ErrorCode errorCode, String resourceName) {
        if (isNull(data)) {
            throw new BusinessResultException(errorCode)
                    .setResource(resourceName);
        }
    }

    /**
     * 对象不为空时抛出业务异常
     *
     * @param data 待检查对象
     * @param errorCode 错误码
     * @param resourceName 资源名称
     * @throws BusinessResultException 对象不为空时抛出
     */
    public static void isNotNullThrowError(Object data, ErrorCode errorCode, String resourceName) {
        if (isNotNull(data)) {
            throw new BusinessResultException(errorCode)
                    .setResource(resourceName);
        }
    }

    /**
     * 对象为空时抛出业务异常（无资源名称）
     *
     * @param data 待检查对象
     * @param errorCode 错误码
     * @throws BusinessResultException 对象为 null 时抛出
     */
    public static void isNullThrowError(Object data, ErrorCode errorCode) {
        if (isNull(data)) {
            throw new BusinessResultException(errorCode);
        }
    }

    /**
     * 对象不为空时抛出业务异常（无资源名称）
     *
     * @param data 待检查对象
     * @param errorCode 错误码
     * @throws BusinessResultException 对象不为空时抛出
     */
    public static void isNotNullThrowError(Object data, ErrorCode errorCode) {
        if (isNotNull(data)) {
            throw new BusinessResultException(errorCode);
        }
    }

    /**
     * 条件校验，条件不满足时抛出系统异常
     *
     * @param condition 校验条件
     * @param message 异常信息
     * @throws BusinessResultException 条件为 false 时抛出（HTTP 500 系统错误）
     */
    public static void checkConditionThrowError(boolean condition, String message) {
        if (condition) {
            return;
        }
        throw new BusinessResultException(message, SystemErrorCode.SYSTEM_ERROR)
                .setHttpCode(SystemErrorCode.SYSTEM_ERROR_DEFAULT_CODE);
    }

    /**
     * 条件校验，条件不满足时抛出指定编码和信息的异常
     *
     * @param condition 校验条件
     * @param code 异常编码
     * @param message 异常信息
     * @throws BusinessResultException 条件为 false 时抛出
     */
    public static void checkConditionThrowError(boolean condition, Integer code, String message) {
        if (condition) {
            return;
        }
        throw new BusinessResultException(code, message);
    }

    /**
     * 条件校验，条件不满足时抛出指定错误码的异常
     *
     * @param condition 校验条件
     * @param errorCode 错误码
     * @param message 异常信息
     * @throws BusinessResultException 条件为 false 时抛出
     */
    public static void checkConditionThrowError(boolean condition, ErrorCode errorCode, String message) {
        if (condition) {
            return;
        }
        throw new BusinessResultException(errorCode).setMessage(message);
    }

}
