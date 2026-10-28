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
 * Exception thrown when a command request to the Sentinel client has failed.
 *
 * @author Eric Zhao
 */
public class CommandFailedException extends RuntimeException {

    /** Create the exception without a message. */
    public CommandFailedException() {}

    /** Creates the exception with the given message. */
    public CommandFailedException(String message) {
        super(message);
    }

    /** {@inheritDoc} Returns this exception directly without filling in the stack trace. */
    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
