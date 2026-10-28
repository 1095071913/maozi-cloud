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
 * Exception thrown when the command is not supported (not found) by the Sentinel client.
 *
 * @author Eric Zhao
 * @since 0.2.1
 */
public class CommandNotFoundException extends Exception {

    /** Create the exception without a message. */
    public CommandNotFoundException() { }

    /** Creates the exception with the given message. */
    public CommandNotFoundException(String message) {
        super(message);
    }

    /** {@inheritDoc} Returns this exception directly without filling in the stack trace. */
    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
