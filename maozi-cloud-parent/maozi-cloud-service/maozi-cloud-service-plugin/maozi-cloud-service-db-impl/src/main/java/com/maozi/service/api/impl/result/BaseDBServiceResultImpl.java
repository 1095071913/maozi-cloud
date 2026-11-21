package com.maozi.service.api.impl.result;

import com.maozi.service.api.impl.BaseServiceImpl;
import com.maozi.service.api.rpc.BaseDBServiceResult;

/**
 * 无 DTO 响应类型的业务结果封装基础实现
 * <p>
 * 结果类型固定为 {@link Void} 的兜底实现，供无具体 DTO 响应类型的业务服务继承，
 * 复用 {@link BaseDBTServiceResultImpl} 的全部通用能力。
 * </p>
 *
 * @author pengjinlong
 * @date 2026/11/20 01:21
 */
@SuppressWarnings({"rawtypes"})
public class BaseDBServiceResultImpl<S extends BaseServiceImpl>
        extends BaseDBTServiceResultImpl<S, Void> implements BaseDBServiceResult<Void> {

}
