package com.maozi.service.api.result;

import com.maozi.base.dto.IntegerArrayList;
import com.maozi.base.dto.StringArrayList;
import com.maozi.base.result.EnumResult;
import com.maozi.common.result.AbstractBaseResult;
import com.maozi.common.result.error.ErrorResult;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 模版结果对象
 * <p>
 * 用于展示接口返回结构的标准模版，包含常用的返回类型示例：
 * 错误结果、字符串列表、整数列表和枚举结果。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
@Data
public class TemplateResult implements Serializable {

    /** 序列化标识 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 错误结果示例 */
    private final ErrorResult<?> errorResult;

    /** 字符串列表示例 */
    private final StringArrayList stringArrayList;

    /** 整数列表示例 */
    private final IntegerArrayList integerArrayList;

    /** 枚举结果示例 */
    private final AbstractBaseResult<EnumResult> baseResultEnumResult;

}
