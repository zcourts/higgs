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
package io.higgs.hmq;

import io.higgs.hmq.protocol.HandshakeHandler;
import io.higgs.hmq.protocol.SocketHandler;
import io.higgs.hmq.protocol.SocketType;
import io.higgs.hmq.protocol.serialization.FrameDecoder;
import io.higgs.hmq.protocol.serialization.FrameEncoder;
import io.higgs.hmq.protocol.serialization.HandshakeDecoder;
import io.higgs.hmq.protocol.serialization.HandshakeEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

public class Client {

    private final String host;
    private final int port;

    public Client(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public static void main(String[] args) throws Exception {

        new Client("localhost", 5563).connect(SocketType.SUB);
    }

    public SocketHandler connect(final SocketType type) throws Exception {
        EventLoopGroup group = new NioEventLoopGroup();
        final Bootstrap b = new Bootstrap();
        final SocketHandler handler = new SocketHandler();
        b.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new HandshakeDecoder(type));
                        ch.pipeline().addLast(new HandshakeEncoder());
                        //
                        ch.pipeline().addLast(new FrameDecoder());
                        ch.pipeline().addLast(new FrameEncoder());
                        //
                        ch.pipeline().addLast(new HandshakeHandler(type, handler));
                        ch.pipeline().addLast(handler);
                    }
                });
        b.connect(host, port).sync();
        return handler;
    }
}
