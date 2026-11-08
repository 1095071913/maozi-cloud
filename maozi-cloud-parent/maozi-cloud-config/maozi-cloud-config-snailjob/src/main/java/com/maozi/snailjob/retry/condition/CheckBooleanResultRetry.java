package com.maozi.snailjob.retry.condition;

import com.aizuda.snailjob.client.core.RetryCondition;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Boolean 结果重试条件
 * <p>
 * 供 {@code @RetryTask} 注解的 {@code retryIfResult} 属性引用：
 * 方法执行结果为 null 或 Boolean false 时判定需要重试，其余情况无需重试。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
@Component
public class CheckBooleanResultRetry implements RetryCondition {

    /**
     * 判断方法执行结果是否需要重试
     *
     * @param returnResult 方法执行结果
     * @return 结果为 null 或 Boolean false 时返回 true（需要重试），其余返回 false
     */
    @Override
    public boolean shouldRetry(Object returnResult) {

        if (Objects.isNull(returnResult)) {
            return true;
        }

        if (returnResult instanceof Boolean booleanResult) {
            return !booleanResult;
        }

        return false;

    }

}
