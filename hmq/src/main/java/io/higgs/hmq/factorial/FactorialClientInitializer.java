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
package io.higgs.hmq.factorial;

import io.higgs.hmq.protocol.SignatureHandler;
import io.higgs.hmq.protocol.SocketHandler;
import io.higgs.hmq.protocol.serialization.FrameDecoder;
import io.higgs.hmq.protocol.serialization.FrameEncoder;
import io.higgs.hmq.protocol.serialization.IdentityEncoder;
import io.higgs.hmq.protocol.serialization.RevisionAndSocketTypeDecoder;
import io.higgs.hmq.protocol.serialization.SignatureDecoder;
import io.higgs.hmq.protocol.serialization.SignatureEncoder;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;

/**
 * Creates a newly configured {@link io.netty.channel.ChannelPipeline} for a client-side channel.
 */
public class FactorialClientInitializer extends ChannelInitializer<SocketChannel> {

    public FactorialClientInitializer() {
    }

    @Override
    public void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();

        pipeline.addLast("signature-decoder", new SignatureDecoder());
        pipeline.addLast("signature-encoder", new SignatureEncoder());
        //
        pipeline.addLast("revision-and-socket-decoder", new RevisionAndSocketTypeDecoder());
        pipeline.addLast("identity-encoder", new IdentityEncoder());
//
        pipeline.addLast("frame-decoder", new FrameDecoder());
        pipeline.addLast("frame-encoder", new FrameEncoder());

        // and then business logic.
        pipeline.addLast("signature-handler", new SignatureHandler());
        //
        pipeline.addLast("socket-handler", new SocketHandler());

        //
//        pipeline.addLast("handler", new FactorialClientHandler(count));
    }
}
