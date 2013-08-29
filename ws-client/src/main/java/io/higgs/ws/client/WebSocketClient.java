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
import io.higgs.events.Events;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class WebSocketClient {
    public static final ObjectMapper MAPPER = new ObjectMapper();
    /**
     * The maximum size a websocket frame can be - in bytes
     */
    public static int maxFramePayloadLength = 65536 * 10;

    /**
     * Connect to the given URI
     *
     * @param uri the URI to connect to
     * @return a channel future which will be notified when the connection has completed
     */
    public static WebSocketStream connect(URI uri) {
        return connect(uri, new HashMap<String, Object>());
    }

    /**
     * Connect to the given URI
     *
     * @param uri           the URI to connect to
     * @param customHeaders any custom headers to use
     * @return a channel future which will be notified when the connection has completed
     */
    public static WebSocketStream connect(URI uri, Map<String, Object> customHeaders) {
        EventLoopGroup group = new NioEventLoopGroup();
        //make all of these parameters
        boolean allowExtensions = false;
        WebSocketVersion version = WebSocketVersion.V13;
        String subprotocol = null;
        //
        String url = uri.toString();
        Events events = Events.group(url); //fresh resources for each url
        Bootstrap b = new Bootstrap();
        final String protocol = uri.getScheme();
        if (!"ws".equals(protocol) && !"wss".equals(protocol)) {
            throw new IllegalArgumentException("Unsupported protocol: " + protocol);
        }

        HttpHeaders customHeaderSet = new DefaultHttpHeaders();
        for (Map.Entry<String, Object> e : (customHeaders == null ? new HashMap<String, Object>() : customHeaders).entrySet()) {
            customHeaderSet.add(e.getKey(), e.getValue());
        }
        WebSocketClientHandshaker handshaker = WebSocketClientHandshakerFactory
                .newHandshaker(uri, WebSocketVersion.V13, subprotocol, allowExtensions, customHeaderSet, maxFramePayloadLength);
        final WebSocketClientHandler handler = new WebSocketClientHandler(handshaker, events);

        b.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
//TODO add secure WebSocket support
//                            if ("wss".equals(protocol)) {
//                                SSLEngine engine = SSLContextFactory.getSSLSocket(SSLConfigFactory.sslConfiguration).createSSLEngine();
//                                engine.setUseClientMode(true);
//                                pipeline.addLast("ssl", new SslHandler(engine));
//                            }
                        pipeline.addLast("http-codec", new HttpClientCodec());
                        pipeline.addLast("aggregator", new HttpObjectAggregator(8192));
                        pipeline.addLast("ws-handler", handler);
                    }
                });
        ChannelFuture cf = b.connect(uri.getHost(), uri.getPort());
        return new WebSocketStream(uri, cf, events);
    }
}
