package com.maozi.dubbo.aop;

import com.maozi.common.LogUtil;
import com.maozi.common.ResultUtil;
import com.maozi.common.constant.LogTag;
import com.maozi.common.constant.SymbolConstant;
import com.maozi.common.context.ApplicationEnvironmentContext;
import com.maozi.common.enums.LogCommonTypeEnum;
import com.maozi.common.result.error.code.ErrorCode;
import com.maozi.common.result.error.code.SystemErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 远程调用异常处理切面
 * <p>
 * 拦截所有 RPC 接口层方法（匹配 {@code api.rpc} 包），
 * 当调用抛出异常时，记录错误日志并返回统一的错误结果，避免异常向上层传播。
 * </p>
 * <p>
 * <b>注：</b>代码中保留了按包路径第 6 段是否为 {@code impl} 判断是否重新抛出异常的
 * 防御逻辑，
 * 但当前切点限定的包路径第 6 段恒为 {@code rpc}，"是接口层调用"的判断恒为真，
 * 因此实际所有被拦截的异常都会被捕获封装，重新抛出的分支当前不可达。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Slf4j
@Aspect
@Component
public class DubboInvokeErrorAop {

    /** impl 包名标识，用于区分接口调用和实现调用 */
    private static final String IMPL = "impl";

    /** 服务名在包路径中的段下标：com.maozi.{服务名}.{模块名}.{层级}.rpc.{类名} */
    private static final int SERVICE_NAME_SEGMENT_INDEX = 2;

    /** 层级段（rpc/impl）在包路径中的段下标：com.maozi.{服务名}.{模块名}.{层级}.rpc.{类名} */
    private static final int RPC_SEGMENT_INDEX = 5;

    /** 包名分隔符切分模式（预编译） */
    private static final Pattern DOT_SPLIT_PATTERN = Pattern.compile("\\.");

    /** RPC 接口切点表达式 */
    private static final String RPC_POINT = ApplicationEnvironmentContext.PACKAGE_PREFIX
            + ".common.result.AbstractBaseResult "
            + ApplicationEnvironmentContext.PACKAGE_PREFIX
            + ".*.*.api.rpc..*.*(..)";

    /** 组合切点表达式 */
    private static final String POINT = "execution(" + RPC_POINT + ")";

    /** 结构化错误日志 Map 的初始容量 */
    private static final int LOG_MAP_INITIAL_CAPACITY = 16;

    /**
     * 环绕通知，捕获远程调用异常
     *
     * @param proceedingJoinPoint AOP 连接点
     * @return 调用结果，异常时返回错误结果
     * @throws Throwable 非接口层调用时向上抛出原始异常
     */
    @Around(POINT)
    public Object doAround(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {

        // 尝试执行目标方法，如果正常返回则直接返回结果
        try {
            return proceedingJoinPoint.proceed();
        } catch (Throwable e) {

            // 缓存方法签名信息与类全限定名，避免同一方法内重复调用
            Signature signature = proceedingJoinPoint.getSignature();
            String declaringTypeName = signature.getDeclaringTypeName();

            // 解析被调用类的全限定名，按 "." 分割以提取包路径中的各段信息
            // 包路径格式约定为：com.maozi.{服务名}.{模块名}.{层级}.rpc.{类名}
            String[] remoteInvokeClassSplit = DOT_SPLIT_PATTERN
                    .split(declaringTypeName);

            // 包路径段数不足（切点调整或类移动后无法按约定解析）时
            // 直接抛出原始异常，避免数组越界
            if (remoteInvokeClassSplit.length <= RPC_SEGMENT_INDEX) {
                throw e;
            }

            // 判断是否为接口层调用：按层级段下标取值，对应包路径中 api 下的子包名。
            // 当前切点限定的子包名恒为 "rpc"，下方条件恒不成立；
            // 若未来切点扩展到 api.impl 包，此处可让实现层内部调用的异常继续向上抛出
            if (IMPL.equals(remoteInvokeClassSplit[RPC_SEGMENT_INDEX])) {
                // impl 层的调用直接抛出原始异常（当前切点下不可达，见类注释说明）
                throw e;
            }

            // 从包路径第3段提取服务名称，例如 "com.maozi.user.xxx" 中的 "user"
            String serviceName = remoteInvokeClassSplit[SERVICE_NAME_SEGMENT_INDEX];

            // 构建结构化的错误日志信息
            Map<String, String> logs = new LinkedHashMap<>(LOG_MAP_INITIAL_CAPACITY);

            // 日志类型：RPC 远程调用
            logs.put(LogTag.TYPE, LogCommonTypeEnum.RPC.getDesc());
            // 被调用的服务名称
            logs.put(LogTag.SERVICE_NAME, serviceName);
            // 被调用的方法全路径
            logs.put(LogTag.FUNCTION, declaringTypeName + SymbolConstant.COLON + signature.getName());
            // 方法入参
            logs.put(LogTag.PARAM, Arrays.toString(proceedingJoinPoint.getArgs()));

            // 记录结构化的错误日志信息
            LogUtil.error(log, logs, e);

            // 构建统一的 RPC 调用错误响应，避免异常向上传播
            ErrorCode errorCode = SystemErrorCode.SERVICE_RPC_ERROR;
            return ResultUtil.error(errorCode)
                    // 设置统一的系统错误 HTTP 状态码
                    .setHttpCode(SystemErrorCode.SYSTEM_ERROR_DEFAULT_CODE);

        }

    }

}
