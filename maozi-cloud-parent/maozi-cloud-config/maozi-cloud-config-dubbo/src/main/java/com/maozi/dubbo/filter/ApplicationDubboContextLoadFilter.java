package com.maozi.dubbo.filter;

import com.maozi.common.context.AbstractRequestContextHolder;
import com.maozi.common.context.CurrentUserInfoContext;
import com.maozi.common.context.TestFlowContext;
import com.maozi.common.context.TraceIdContext;
import com.maozi.common.context.VersionContext;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcContextAttachment;
import org.apache.dubbo.rpc.RpcException;

/**
 * Dubbo 服务端链路上下文设置过滤器
 * <p>
 * 在 Dubbo 服务提供者端接收请求时，从 RPC 附件（Attachment）中提取消费者传递的
 * 版本号、测试流量标记、当前登录用户信息和链路追踪 ID，分别设置到当前线程的
 * {@link VersionContext}、{@link TestFlowContext}、{@link CurrentUserInfoContext}、
 * {@link TraceIdContext} 中，供后续业务逻辑获取调用方的上下文信息。
 * </p>
 * <p>
 * 请求处理完成后，在 finally 块中自动清理线程上下文，
 * 防止 Dubbo 线程池复用时上下文数据泄漏到其他请求。
 * </p>
 * <p>
 * 该过滤器通过 Dubbo SPI 机制加载，需在 resources/META-INF/dubbo 目录下配置。
 * </p>
 *
 * @author maozi
 * @date 2026/11/13
 * @see ApplicationDubboContextTransmitFilter
 * @see Filter
 */
public class ApplicationDubboContextLoadFilter implements Filter {

    /**
     * 拦截 Dubbo 服务端调用，从 RPC 附件中提取链路上下文设置到当前线程，
     * 调用结束后清理线程上下文，防止线程池复用时数据泄漏
     *
     * @param invoker    Dubbo 调用器，封装了被调用的服务信息
     * @param invocation 调用信息，包含方法名、参数类型、参数值等
     * @return 调用结果 {@link Result}，包含返回值或异常信息
     * @throws RpcException RPC 调用异常
     */
    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {

        // 获取服务端的 RPC 附件对象，用于读取消费者传递的附件信息
        RpcContextAttachment serverAttachment = RpcContext.getServerAttachment();

        // 从 RPC 附件中提取版本号，设置到当前线程的版本上下文中
        VersionContext.CONTEXT.set(serverAttachment.getAttachment(VersionContext.KEY));

        // 从 RPC 附件中提取测试流量标记，设置到当前线程的测试流量上下文中
        TestFlowContext.CONTEXT.set(serverAttachment.getAttachment(TestFlowContext.KEY));

        // 从 RPC 附件中提取当前登录用户信息 JSON，反序列化后设置到当前线程的用户上下文中
        CurrentUserInfoContext.CONTEXT.set(serverAttachment.getAttachment(CurrentUserInfoContext.KEY));

        // 从 RPC 附件中提取链路追踪 ID，设置到当前线程的链路追踪上下文中（非空时同步写入 MDC）
        TraceIdContext.CONTEXT.set(serverAttachment.getAttachment(TraceIdContext.KEY));

        // 执行实际调用，在 finally 中清理上下文，确保即使发生异常也不会造成线程数据泄漏
        try{return invoker.invoke(invocation);} finally{
            AbstractRequestContextHolder.clearContext();
        }

    }

}
