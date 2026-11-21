package com.maozi.service.api.impl.result;

import com.maozi.service.api.impl.BaseServiceImpl;
import com.maozi.service.api.rpc.BaseDBServiceResult;

/**
 * @author pengjinlong
 * @date 2026/11/20 01:21
 */
@SuppressWarnings({"rawtypes"})
public class BaseDBServiceResultImpl<S extends BaseServiceImpl>
        extends BaseDBTServiceResultImpl<S, Void> implements BaseDBServiceResult<Void> {

}
