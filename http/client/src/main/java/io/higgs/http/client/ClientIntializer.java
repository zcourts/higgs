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

import io.higgs.core.ssl.SSLConfigFactory;
import io.higgs.core.ssl.SSLContextFactory;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedWriteHandler;

import javax.net.ssl.SSLEngine;

public class ClientIntializer extends ChannelInitializer<SocketChannel> {
    protected final boolean ssl;
    protected final ConnectHandler connectHandler;
    protected final SimpleChannelInboundHandler<Object> handler;
    protected final String[] sslProtocols;

    public ClientIntializer(boolean ssl, SimpleChannelInboundHandler<Object> handler, ConnectHandler connectHandler,
                            String[] sslProtocols) {
        this.ssl = ssl;
        this.handler = handler;
        this.connectHandler = connectHandler;
        this.sslProtocols = sslProtocols;
    }

    @Override
    public void initChannel(SocketChannel ch) throws Exception {
        // Create a default pipeline implementation.
        ChannelPipeline pipeline = ch.pipeline();
        configurePipeline(pipeline);
    }

    public void configurePipeline(ChannelPipeline pipeline) {
        if (ssl) {
            addSSL(pipeline, false, sslProtocols);
        }

        if (pipeline.get("codec") == null) {
            pipeline.addLast("codec", new HttpClientCodec());
        } else {
            pipeline.replace("codec", "codec", new HttpClientCodec());
        }
        if (pipeline.get("inflater") == null) {
            pipeline.addLast("inflater", new HttpContentDecompressor());
        } else {
            pipeline.replace("inflater", "inflater", new HttpContentDecompressor());
        }
        if (pipeline.get("chunkedWriter") == null) {
            pipeline.addLast("chunkedWriter", new ChunkedWriteHandler());
        } else {
            pipeline.replace("chunkedWriter", "chunkedWriter", new ChunkedWriteHandler());
        }
        //if a connect handler is provided then add it otherwise add the normal response handler
        if (pipeline.get("handler") == null) {
            pipeline.addLast("handler", connectHandler == null ? handler : connectHandler);
        } else {
            pipeline.replace("handler", "handler", connectHandler == null ? handler : connectHandler);
        }
    }

    /**
     * Adds an SSL engine to the given pipeline.
     *
     * @param pipeline     the pipeline to add SSL support to
     * @param forceToFront if true then the SSL handler is added to the front of the pipeline otherwise it is added
     *                     at the end
     */
    public static void addSSL(ChannelPipeline pipeline, boolean forceToFront, String[] sslProtocols) {
        SSLEngine engine = SSLContextFactory.getSSLSocket(SSLConfigFactory.sslConfiguration).createSSLEngine();
        engine.setUseClientMode(true);
        if (sslProtocols != null && sslProtocols.length > 0) {
            engine.setEnabledProtocols(sslProtocols);
        }
        if (forceToFront) {
            pipeline.addFirst("ssl", new SslHandler(engine));
        } else {
            pipeline.addLast("ssl", new SslHandler(engine));
        }
    }
}
