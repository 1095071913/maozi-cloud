package com.maozi.common.result.error.exception;

import com.maozi.common.ResultUtil;
import com.maozi.common.result.error.ErrorResult;
import com.maozi.common.result.error.code.ErrorCode;
import lombok.Data;

import java.io.Serial;

/**
 * 业务结果异常
 * <p>
 * 封装错误结果信息的运行时异常，携带 {@link ErrorResult} 对象。
 * 所有业务校验失败、系统异常等场景均通过抛出此异常来中断流程，
 * 由全局异常处理器统一捕获并返回标准错误响应。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Data
public class BusinessResultException extends RuntimeException {

    /** 序列化标识 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 错误结果对象 */
    private ErrorResult<?> errorResult;

    /**
     * 根据错误信息构造
     *
     * @param message 错误信息
     */
    public BusinessResultException(String message) {
        super(message);
        this.errorResult = ResultUtil.error(new ErrorCode(message));
    }

    /**
     * 根据错误码和错误信息构造
     *
     * @param code 错误编码
     * @param message 错误信息
     */
    public BusinessResultException(Integer code,String message) {
        super(message);
        this.errorResult = ResultUtil.error(new ErrorCode(code,message));
    }

    /**
     * 根据错误码、错误信息和附加数据构造
     *
     * @param code 错误编码
     * @param message 错误信息
     * @param data 错误附加数据
     * @param <D> 数据类型
     */
    public <D> BusinessResultException(Integer code,String message,D data) {
        super(message);
        this.errorResult = ResultUtil.error(new ErrorCode(code,message),data);
    }

    /**
     * 根据错误码对象构造
     *
     * @param errorCode 错误码
     */
    public BusinessResultException(ErrorCode errorCode) {
        super(errorCode.getExceptionMessage());
        this.errorResult = ResultUtil.error(errorCode);
    }

    /**
     * 根据错误码对象和原始异常构造
     * <p>
     * 保留原始异常堆栈，保证异常排查链路完整。
     * </p>
     *
     * @param errorCode 错误码
     * @param cause 原始异常
     */
    public BusinessResultException(ErrorCode errorCode,Throwable cause) {
        super(errorCode.getExceptionMessage(),cause);
        this.errorResult = ResultUtil.error(errorCode);
    }

    /**
     * 根据错误码对象和附加数据构造
     *
     * @param errorCode 错误码
     * @param errorData 错误附加数据
     * @param <D> 数据类型
     */
    public <D> BusinessResultException(ErrorCode errorCode,D errorData) {
        super(errorCode.getExceptionMessage());
        this.errorResult = ResultUtil.error(errorCode,errorData);
    }

    /**
     * 根据异常信息和错误码构造
     *
     * @param exceptionMessage 异常信息
     * @param errorCode 错误码
     */
    public BusinessResultException(String exceptionMessage,ErrorCode errorCode) {
        super(exceptionMessage);
        this.errorResult = ResultUtil.error(errorCode).setExceptionMessage(exceptionMessage);
    }

    /**
     * 根据异常信息、错误码和附加数据构造
     *
     * @param exceptionMessage 异常信息
     * @param errorCode 错误码
     * @param errorData 错误附加数据
     * @param <D> 数据类型
     */
    public <D> BusinessResultException(String exceptionMessage, ErrorCode errorCode, D errorData) {
        super(exceptionMessage);
        this.errorResult = ResultUtil.error(errorCode,errorData).setExceptionMessage(exceptionMessage);
    }

    /**
     * 根据错误结果对象构造
     *
     * @param errorResult 错误结果
     * @param <T> 数据类型
     */
    public <T> BusinessResultException(ErrorResult<T> errorResult) {
        super(errorResult.getExceptionMessage());
        this.errorResult = errorResult;
    }

    /**
     * 根据异常信息和错误结果对象构造
     *
     * @param exceptionMessage 异常信息
     * @param errorResult 错误结果
     * @param <T> 数据类型
     */
    public <T> BusinessResultException(String exceptionMessage,ErrorResult<T> errorResult) {
        super(exceptionMessage);
        this.errorResult = errorResult.setExceptionMessage(exceptionMessage);
    }

    /**
     * 将资源名称作为前缀拼接到错误信息前
     *
     * @param resource 资源名称
     * @return 当前实例（链式调用）
     */
    public BusinessResultException setResource(String resource) {
        this.errorResult.setMessage(resource + this.errorResult.getMessage());
        return this;
    }

    /**
     * 设置错误结果中的错误信息
     *
     * @param message 错误信息
     * @return 当前实例（链式调用）
     */
    public BusinessResultException setMessage(String message) {
        this.errorResult.setMessage(message);
        return this;
    }

    /**
     * 同时设置错误结果中的错误信息和异常错误信息
     *
     * @param message 错误信息
     * @param exceptionMessage 异常错误信息
     * @return 当前实例（链式调用）
     */
    public BusinessResultException setMessage(String message,String exceptionMessage) {
        this.errorResult.setMessage(message,exceptionMessage);
        return this;
    }

    /**
     * 设置错误结果中的 HTTP 状态码
     *
     * @param httpCode HTTP 状态码
     * @return 当前实例（链式调用）
     */
    public BusinessResultException setHttpCode(Integer httpCode) {
        this.errorResult.setHttpCode(httpCode);
        return this;
    }

}
