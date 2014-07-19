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
//The MIT License
//
//Copyright (c) 2009 Carl Bystršm
//
//Permission is hereby granted, free of charge, to any person obtaining a copy
//of this software and associated documentation files (the "Software"), to deal
//in the Software without restriction, including without limitation the rights
//to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
//copies of the Software, and to permit persons to whom the Software is
//furnished to do so, subject to the following conditions:
//
//The above copyright notice and this permission notice shall be included in
//all copies or substantial portions of the Software.
//
//THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//THE SOFTWARE.
package io.higgs.ws.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.higgs.core.StaticUtil;
import io.higgs.http.client.ClientIntializer;
import io.higgs.http.client.ConnectHandler;
import io.higgs.http.client.FutureResponse;
import io.higgs.http.client.HttpRequestBuilder;
import io.higgs.http.client.Request;
import io.higgs.http.client.readers.PageReader;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import org.cliffc.high_scale_lib.NonBlockingHashSet;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class WebSocketClient extends Request {
    public static final ObjectMapper MAPPER = new ObjectMapper();
    protected static final HttpRequestBuilder BUILDER = HttpRequestBuilder.instance();
    /**
     * The maximum size a websocket frame can be - in bytes
     */
    public static int maxFramePayloadLength = 65536 * 10;
    protected final WebSocketClientHandler handler;
    protected NonBlockingHashSet<WebSocketEventListener> listeners = new NonBlockingHashSet<>();
    protected WebSocketClientHandshaker handshaker;
    protected boolean allowExtensions;
    protected WebSocketVersion version = WebSocketVersion.V13;
    protected String subprotocol;
    protected HttpHeaders customHeaderSet = new DefaultHttpHeaders();
    protected int maxContentLength = 8192;
    protected WebSocketStream stream;
    protected boolean autoPong = true;
    protected String[] sslProtocols;

    public WebSocketClient(URI uri, Map<String, Object> customHeaders, boolean autoPong, String[] sslProtocols) {
        super(BUILDER, HttpRequestBuilder.group(), uri, HttpMethod.GET, HttpVersion.HTTP_1_1, new PageReader());
        final String protocol = uri.getScheme();
        if (!"ws".equals(protocol) && !"wss".equals(protocol)) {
            throw new IllegalArgumentException("Unsupported protocol: " + protocol);
        }
        if (customHeaders != null) {
            for (Map.Entry<String, Object> e : customHeaders.entrySet()) {
                customHeaderSet.add(e.getKey(), e.getValue());
            }
        }
        handshaker = WebSocketClientHandshakerFactory.newHandshaker(uri, version, subprotocol, allowExtensions,
                customHeaderSet, maxFramePayloadLength);
        handler = new WebSocketClientHandler(handshaker, listeners, autoPong);
        this.autoPong = autoPong;
        this.sslProtocols = sslProtocols == null || sslProtocols.length == 0 ?
                HttpRequestBuilder.getSupportedSSLProtocols() : sslProtocols;
    }

    /**
     * Connect to the given URI
     *
     * @param uri          the URI to connect to
     * @param autoPong     if true then the client automatically responds to pings by sending a pong
     * @param sslProtocols a list of SSL protocols to support or null to use all available options on this JVM
     * @return a channel future which will be notified when the connection has completed
     */
    public static WebSocketStream connect(URI uri, boolean autoPong, String[] sslProtocols) {
        return connect(uri, new HashMap<String, Object>(), autoPong, sslProtocols);
    }

    /**
     * Connect to the given URI
     *
     * @param uri           the URI to connect to
     * @param customHeaders any custom headers to use
     * @param autoPong      if true then the client automatically responds to pings by sending a pong
     * @param sslProtocols  a list of SSL protocols to support or null to use all available options on this JVM
     * @return a channel future which will be notified when the connection has completed
     */
    public static WebSocketStream connect(URI uri, Map<String, Object> customHeaders, boolean autoPong,
                                          String[] sslProtocols) {
        WebSocketClient client = new WebSocketClient(uri, customHeaders, autoPong, sslProtocols);
        client.execute();
        return client.stream();
    }

    public FutureResponse execute() {
        FutureResponse res = super.execute();
        this.stream = new WebSocketStream(uri, connectFuture, listeners);
        return res;
    }

    public WebSocketStream stream() {
        return stream;
    }

    protected String getScheme() {
        return uri.getScheme() == null ? "ws" : uri.getScheme();
    }

    protected String getHost() {
        return uri.getHost() == null ? "localhost" : uri.getHost();
    }

    protected boolean isSSLScheme(String scheme) {
        return "wss".equalsIgnoreCase(scheme);
    }

    protected ChannelHandler newInitializer() {
        final String fullUrl = isProxyEnabled() && !tunneling ? request.getUri() : null;

        ConnectHandler.InitFactory factory = new ConnectHandler.InitFactory() {
            @Override
            public ClientIntializer newInstance(boolean ssl,
                                                SimpleChannelInboundHandler<Object> handler,
                                                ConnectHandler h) {
                return new WebSocketInitializer(maxContentLength, ssl, handler, h, fullUrl, sslProtocols);
            }
        };

        final WebSocketClientHandler wsh = handler;
        ConnectHandler connectHandler = new

                ConnectHandler(tunneling, request, handler, factory) {
                    protected void writeOriginalRequest(ChannelHandlerContext ctx) {
                        wsh.doHandshake(ctx);
                    }
                };

        connectHandler = isProxyEnabled() && proxyRequest != null ? connectHandler : null;

        return new WebSocketInitializer(maxContentLength, useSSL, handler, connectHandler, fullUrl, sslProtocols);
    }

    protected ChannelFuture makeTheRequest() {
        if (isProxyEnabled() && proxyRequest != null) {
            return StaticUtil.write(channel, proxyRequest);
        }
        //don't write the normal HTTP request...
        return null;
    }

    protected SimpleChannelInboundHandler<Object> newInboundHandler() {
        return handler;
    }
}
