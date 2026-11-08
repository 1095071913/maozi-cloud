package com.maozi.snailjob.job.config.context;

import com.alibaba.ttl.TransmittableThreadLocal;

/**
 * SnailJob 任务参数上下文
 * <p>
 * 保存当前任务的业务参数，由 {@code SnailJobEntranceLogAop} 在任务派发阶段
 * （{@code AbstractJobExecutor#jobExecute} 入口）解析 {@code JobContext} 后写入，
 * 任务方法执行时通过 {@link #getJobParams()} 直接读取当前任务参数。
 * </p>
 * <p>
 * 跨线程传递机制：任务执行线程池由 SnailJob 在派发线程上（本上下文写入之后）
 * 按任务批次创建，{@code TransmittableThreadLocal} 继承
 * {@code InheritableThreadLocal}，池线程创建时继承当前值快照，
 * 故任务执行线程可读取到派发阶段写入的参数。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
public class SnailJobParamContext {

    /** 当前任务业务参数（SnailJob 服务端配置的任务参数） */
    private static final TransmittableThreadLocal<String> jobParams = new TransmittableThreadLocal<>();

    /**
     * 获取当前任务业务参数
     *
     * @return 当前任务业务参数；未处于任务执行上下文或任务未配置参数时返回 null
     */
    public static String getJobParams() {
        return jobParams.get();
    }

    /**
     * 设置当前任务业务参数
     *
     * @param param 任务业务参数
     */
    public static void setJobParams(String param) {
        jobParams.set(param);
    }

    /**
     * 清除当前线程的任务业务参数
     */
    public static void clearJobParams() {
        jobParams.remove();
    }

}
