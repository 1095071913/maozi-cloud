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
 * @author leyou
 * @author Eric Zhao
 */
public class Result<R> {

    /** Whether the operation is successful. */
    private boolean success;
    /** The result code. */
    private int code;
    /** The result message. */
    private String msg;
    /** The payload data of the result. */
    private R data;

    /** Create a successful result carrying the given data and msg "success". */
    public static <R> Result<R> ofSuccess(R data) {
        return new Result<R>()
            .setSuccess(true)
            .setMsg("success")
            .setData(data);
    }

    /** Create a successful result carrying the given message. */
    public static <R> Result<R> ofSuccessMsg(String msg) {
        return new Result<R>()
            .setSuccess(true)
            .setMsg(msg);
    }

    /** Create a failed result with the given code and message. */
    public static <R> Result<R> ofFail(int code, String msg) {
        Result<R> result = new Result<>();
        result.setSuccess(false);
        result.setCode(code);
        result.setMsg(msg);
        return result;
    }

    /**
     * Create a failed result whose message combines the class name and message
     * of the given throwable.
     */
    public static <R> Result<R> ofThrowable(int code, Throwable throwable) {
        Result<R> result = new Result<>();
        result.setSuccess(false);
        result.setCode(code);
        result.setMsg(throwable.getClass().getName() + ", " + throwable.getMessage());
        return result;
    }

    /** Whether the operation is successful. */
    public boolean isSuccess() {
        return success;
    }

    /** Set whether the operation is successful and return this. */
    public Result<R> setSuccess(boolean success) {
        this.success = success;
        return this;
    }

    /** Get the result code. */
    public int getCode() {
        return code;
    }

    /** Set the result code and return this. */
    public Result<R> setCode(int code) {
        this.code = code;
        return this;
    }

    /** Get the result message. */
    public String getMsg() {
        return msg;
    }

    /** Set the result message and return this. */
    public Result<R> setMsg(String msg) {
        this.msg = msg;
        return this;
    }

    /** Get the payload data. */
    public R getData() {
        return data;
    }

    /** Set the payload data and return this. */
    public Result<R> setData(R data) {
        this.data = data;
        return this;
    }

    /** Get the string representation of the result. */
    @Override
    public String toString() {
        return "Result{" +
            "success=" + success +
            ", code=" + code +
            ", msg='" + msg + '\'' +
            ", data=" + data +
            '}';
    }
}
