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
package io.higgs.http.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundMessageHandlerAdapter;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

public class ClientHandler extends ChannelInboundMessageHandlerAdapter<Object> {

    private final Response response;
    private final FutureResponse future;
    private boolean redirecting;

    public ClientHandler(Response response, FutureResponse future) {
        this.future = future;
        this.response = response;
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (redirecting) {
            return;
        }
        if (msg instanceof HttpResponse) {
            HttpResponse res = (HttpResponse) msg;
            String location = res.headers().get(HttpHeaders.Names.LOCATION);
            if (response.request().redirectOn().contains(res.getStatus().code())
                    && location != null) {
                //execute a new request using the same request instance and response
                //this will use a new channel initializer
                response.request()
                        .url(location)
                        .execute()
                        .addListener(new GenericFutureListener<Future<Response>>() {
                            public void operationComplete(Future<Response> f) throws Exception {
                                if (!f.isSuccess()) {
                                    future.setFailure(f.cause());
                                }
                            }
                        });
                redirecting = true;
                return;
            }
            response.setStatus(res.getStatus());
            response.setProtocolVersion(res.getProtocolVersion());
            response.setHeaders(res.headers());

            if (res.getStatus().code() == 200 && HttpHeaders.isTransferEncodingChunked(res)) {
                response.setChunked(true);
            }
        }
        if (msg instanceof HttpContent) {
            HttpContent chunk = (HttpContent) msg;
            response.write(chunk.content());
            if (chunk instanceof LastHttpContent) {
                response.setCompleted(true);
                future.setSuccess(response);
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        future.setFailure(cause);
        ctx.channel().close();
    }
}
