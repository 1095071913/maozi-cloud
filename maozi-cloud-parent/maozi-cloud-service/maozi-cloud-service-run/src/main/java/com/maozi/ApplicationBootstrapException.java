package com.maozi;

import java.io.Serial;

/**
 * 应用启动异常
 * <p>
 * 应用启动阶段发生致命错误（服务名解析失败、运行时配置初始化器
 * 加载或执行失败等）时抛出，直接终止启动流程。
 * 与业务异常 {@code BusinessResultException} 区分，
 * 避免调用方只能笼统捕获顶层 RuntimeException。
 * </p>
 *
 * @author maozi
 * @date 2026/11/21
 */
public class ApplicationBootstrapException extends RuntimeException {

    /** 序列化标识 */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 根据错误信息构造
     *
     * @param message 错误信息
     */
    public ApplicationBootstrapException(String message) {
        super(message);
    }

    /**
     * 根据错误信息和原始异常构造
     * <p>
     * 保留原始异常堆栈，保证异常排查链路完整。
     * </p>
     *
     * @param message 错误信息
     * @param cause 原始异常
     */
    public ApplicationBootstrapException(String message, Throwable cause) {
        super(message, cause);
    }
}
