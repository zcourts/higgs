package com.fillta.higgs.boson;

import com.fillta.higgs.*;
import com.fillta.higgs.boson.serialization.v1.BosonReader;
import com.fillta.higgs.boson.serialization.v1.BosonWriter;
import com.fillta.higgs.util.Function1;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class BosonClient extends HiggsClient<String, BosonMessage, BosonMessage, ByteBuf> {
    @Override
    public MessageConverter<BosonMessage, BosonMessage, ByteBuf> serializer() {
        return new MessageConverter<BosonMessage, BosonMessage, ByteBuf>() {
            @Override
            public ByteBuf serialize(Channel ctx, BosonMessage msg) {
                return new BosonWriter(msg).serialize();
            }

            @Override
            public BosonMessage deserialize(ChannelHandlerContext ctx, ByteBuf msg) {
                return new BosonReader(msg).deSerialize();
            }
        };
    }

    @Override
    public MessageTopicFactory<String, BosonMessage> topicFactory() {
        return new MessageTopicFactory<String, BosonMessage>() {
            @Override
            public String extract(BosonMessage msg) {
                return msg.method;
            }
        };
    }

    public BosonInitializer initializer(boolean inflate, boolean deflate, boolean ssl) {
        return new BosonInitializer(this, inflate, deflate, ssl);
    }

    public void connect(String serviceName, String host, int port,
                        Function1<BosonClientRequest> function) {
        connect(serviceName, host, port, false, false, function);
    }

    public void connect(String serviceName, String host, int port,
                        boolean decompress, boolean useSSL,
                        Function1<BosonClientRequest> function) {
        // connects with a new initializer
        connect(serviceName, host, port, decompress, useSSL,
                initializer(decompress, decompress, useSSL), function);
    }

    @Override
    protected <H extends HiggsClientRequest<String, BosonMessage, BosonMessage, ByteBuf>> H newClientRequest(HiggsClient<String, BosonMessage, BosonMessage, ByteBuf> client, String serviceName, String host, int port, boolean decompress, boolean useSSL, HiggsInitializer initializer) {
        return (H) new BosonClientRequest(
                client, serviceName, host, port, decompress, useSSL, initializer
        );
    }
}
