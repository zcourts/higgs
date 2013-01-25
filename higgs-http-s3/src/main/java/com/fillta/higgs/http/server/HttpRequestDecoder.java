/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.fillta.higgs.http.server;

import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;


public class HttpRequestDecoder extends io.netty.handler.codec.http.HttpRequestDecoder {

    @Override
    protected HttpMessage createMessage(String[] initialLine) throws Exception {
        return new HttpRequest(
                HttpVersion.valueOf(initialLine[2]), HttpMethod.valueOf(initialLine[0]), initialLine[1]);
    }

    @Override
    protected HttpMessage createInvalidMessage() {
        return new HttpRequest(HttpVersion.HTTP_1_0, HttpMethod.GET, "/bad-request");
    }
}
