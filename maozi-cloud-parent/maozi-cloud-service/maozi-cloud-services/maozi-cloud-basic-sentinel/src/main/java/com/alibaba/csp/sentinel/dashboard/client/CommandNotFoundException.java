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
 * Sentinel 客户端不支持（未找到）指定命令时抛出的异常。
 *
 * @author Eric Zhao
 * @date 2026/11/22
 * @since 0.2.1
 */
public class CommandNotFoundException extends Exception {

    private static final long serialVersionUID = 1L;

    /** 构造无详细消息的异常。 */
    public CommandNotFoundException() { }

    /** 使用指定详细消息构造异常。 */
    public CommandNotFoundException(String message) {
        super(message);
    }

    /** {@inheritDoc} 直接返回本异常，不填充堆栈以减少开销。 */
    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
