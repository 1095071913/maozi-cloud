/*
 * Copyright 1999-2018 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.csp.sentinel.dashboard.domain;

/**
 * 控制台 REST 接口的通用返回结果包装类。
 *
 * @author leyou
 * @author Eric Zhao
 * @date 2026/11/22
 */
public class Result<R> {

    /** 成功结果的默认消息。 */
    public static final String DEFAULT_SUCCESS_MSG = "success";

    /** 错误码：请求参数非法或执行出错。 */
    public static final int CODE_ERROR = -1;

    /** 错误码：操作失败，如删除、版本查询失败等。 */
    public static final int CODE_FAIL = 1;

    /** 操作是否成功。 */
    private boolean success;
    /** 结果码。 */
    private int code;
    /** 结果消息。 */
    private String msg;
    /** 结果携带的数据。 */
    private R data;

    /** 创建携带指定数据、消息为 "success" 的成功结果。 */
    public static <R> Result<R> ofSuccess(R data) {
        return new Result<R>()
                .setSuccess(true)
                .setMsg(DEFAULT_SUCCESS_MSG)
                .setData(data);
    }

    /** 创建携带指定消息的成功结果。 */
    public static <R> Result<R> ofSuccessMsg(String msg) {
        return new Result<R>()
                .setSuccess(true)
                .setMsg(msg);
    }

    /** 创建携带指定错误码和消息的失败结果。 */
    public static <R> Result<R> ofFail(int code, String msg) {
        Result<R> result = new Result<>();
        result.setSuccess(false);
        result.setCode(code);
        result.setMsg(msg);
        return result;
    }

    /** 创建消息由异常类名和异常信息组合而成的失败结果。 */
    public static <R> Result<R> ofThrowable(int code, Throwable throwable) {
        Result<R> result = new Result<>();
        result.setSuccess(false);
        result.setCode(code);
        result.setMsg(throwable.getClass().getName() + ", " + throwable.getMessage());
        return result;
    }

    /** 操作是否成功。 */
    public boolean isSuccess() {
        return success;
    }

    /** 设置操作是否成功并返回当前对象。 */
    public Result<R> setSuccess(boolean success) {
        this.success = success;
        return this;
    }

    /** 获取结果码。 */
    public int getCode() {
        return code;
    }

    /** 设置结果码并返回当前对象。 */
    public Result<R> setCode(int code) {
        this.code = code;
        return this;
    }

    /** 获取结果消息。 */
    public String getMsg() {
        return msg;
    }

    /** 设置结果消息并返回当前对象。 */
    public Result<R> setMsg(String msg) {
        this.msg = msg;
        return this;
    }

    /** 获取结果携带的数据。 */
    public R getData() {
        return data;
    }

    /** 设置结果携带的数据并返回当前对象。 */
    public Result<R> setData(R data) {
        this.data = data;
        return this;
    }

    /** 返回结果的字符串表示。 */
    @Override
    public String toString() {
        return "Result{"
                + "success=" + success
                + ", code=" + code
                + ", msg='" + msg + '\''
                + ", data=" + data
                + '}';
    }
}
