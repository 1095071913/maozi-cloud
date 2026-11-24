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
package com.alibaba.csp.sentinel.dashboard.client;

/**
 * 向 Sentinel 客户端发送命令请求失败时抛出的异常。
 *
 * @author Eric Zhao
 * @date 2026/11/22
 */
public class CommandFailedException extends RuntimeException {

    /** 序列化版本号 */
    private static final long serialVersionUID = 1L;

    /** 构造无详细消息的异常。 */
    public CommandFailedException() {}

    /** 使用指定详细消息构造异常。 */
    public CommandFailedException(String message) {
        super(message);
    }

    /** {@inheritDoc} 直接返回本异常，不填充堆栈以减少开销。 */
    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
