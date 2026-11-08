package com.maozi.common.result.error.code;

/**
 * 系统错误码常量类
 * <p>
 * 定义系统中所有通用的错误码，包括系统级异常（编码 0-99999）和
 * 通用业务异常（编码 100000+）。系统级异常涵盖网络、参数、认证、
 * 权限、限流等场景；通用业务异常涵盖禁用、数据不存在、数据已存在等场景。
 * </p>
 *
 * @author maozi
 */
public class SystemErrorCode extends AbstractBaseErrorCode{

    /** 业务异常默认码 */
    public static final Integer BUSINESS_ERROR_DEFAULT_CODE = 400;

    /** 系统异常默认码 */
    public static final Integer SYSTEM_ERROR_DEFAULT_CODE = 500;

    /** 用户认证授权异常默认码 */
    public static final Integer USER_AUTH_ERROR_DEFAULT_CODE = 401;

    /** 限流异常默认码 */
    public static final Integer CURRENT_LIMITING_ERROR_DEFAULT_CODE = 429;

    /** 系统最大错误码，大于此值的为业务错误 */
    public static final Integer MAX_SYSTEM_ERROR_CODE = 99999;

    /** 默认系统异常（HTTP 500，对外提示「服务网络异常」） */
    public static final ErrorCode DEFAULT_ERROR = new ErrorCode(SYSTEM_ERROR_DEFAULT_CODE,"服务网络异常");

    /** 无此错误编码（code=0，对外表现为「服务网络异常」，内部异常信息为「没有此编码」），用于错误码未定义的兜底 */
    public static final ErrorCode NOT_EXIST_CODE_ERROR = new ErrorCode(0,"服务网络异常","没有此编码");

    /** 服务调用失败 */
    public static final ErrorCode SERVICE_RPC_ERROR = new ErrorCode(1,"服务网络异常","服务调用失败");

    /** 服务不存在 */
    public static final ErrorCode SERVICE_NOT_EXIST_ERROR = new ErrorCode(2,"服务网络异常","服务不存在");

    /** 未设置资源返回类型 */
    public static final ErrorCode NOT_SET_RESPONSE_ERROR = new ErrorCode(3,"服务网络异常","未设置资源返回类型");

    /** 参数错误 */
    public static final ErrorCode PARAM_ERROR = new ErrorCode(BUSINESS_ERROR_DEFAULT_CODE,"参数错误");

    /** 用户认证授权失败 */
    public static final ErrorCode USER_AUTH_ERROR = new ErrorCode(USER_AUTH_ERROR_DEFAULT_CODE,"用户认证授权失败");

    /** 恶意请求 */
    public static final ErrorCode MALICE_REQUEST_ERROR = new ErrorCode(402,"恶意请求");

    /** 权限不足 */
    public static final ErrorCode PERMISSION_ERROR = new ErrorCode(403,"权限不足");

    /** 黑名单 */
    public static final ErrorCode BLACKLIST_ERROR = new ErrorCode(403,"黑名单无法访问");

    /** 接口不存在 */
    public static final ErrorCode NOT_RESOURCE_ERROR = new ErrorCode(404,"接口不存在");

    /** 请求方法错误 */
    public static final ErrorCode REQUEST_METHOD_ERROR = new ErrorCode(405,"请求方法错误");

    /** 请求数据过大错误 */
    public static final ErrorCode REQUEST_EXCESSIVE_ERROR = new ErrorCode(413,"请求数据过大");

    /** 请求格式错误 */
    public static final ErrorCode REQUEST_FORMAT_ERROR = new ErrorCode(415,"请求格式错误");

    /** 请求限流中 */
    public static final ErrorCode CURRENT_LIMITING_ERROR = new ErrorCode(CURRENT_LIMITING_ERROR_DEFAULT_CODE,"请求限流中");

    /** 系统异常（同 DEFAULT_ERROR） */
    public static final ErrorCode SYSTEM_ERROR = DEFAULT_ERROR;

    /** 已被禁用 */
    public static final ErrorCode FORBIDDEN_ERROR = new ErrorCode(100000,"已被禁用");

    /** 资源不存在 */
    public static final ErrorCode DATA_NOT_EXIST_ERROR = new ErrorCode(100001,"资源不存在");

    /** 资源已存在 */
    public static final ErrorCode DATA_EXIST_ERROR = new ErrorCode(100002,"资源已存在");

}
