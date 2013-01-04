package com.fillta.higgs.boson;

import com.fillta.higgs.*;
import com.fillta.higgs.boson.serialization.v1.BosonReader;
import com.fillta.higgs.boson.serialization.v1.BosonWriter;
import com.fillta.functional.Function1;
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
			public ByteBuf serialize(Channel ctx, BosonMessage msg) {
				return new BosonWriter(msg).serialize();
			}

			public BosonMessage deserialize(ChannelHandlerContext ctx, ByteBuf msg) {
				return new BosonReader(msg).deSerialize();
			}
		};
	}

	public MessageTopicFactory<String, BosonMessage> topicFactory() {
		return new MessageTopicFactory<String, BosonMessage>() {
			@Override
			public String extract(BosonMessage msg) {
				return msg.method;
			}
		};
	}

	public BosonInitializer newInitializer(boolean inflate, boolean deflate, boolean ssl) {
		return new BosonInitializer(this, inflate, deflate, ssl);
	}

	public void connect(String serviceName, String host, int port,
	                    Function1<BosonClientConnection> function) {
		connect(serviceName, host, port, false, false, function);
	}

	public void connect(String serviceName, String host, int port,
	                    boolean decompress, boolean useSSL,
	                    Function1<BosonClientConnection> function) {
		// connects with a new newInitializer
		connect(serviceName, host, port, decompress, useSSL,
				newInitializer(decompress, decompress, useSSL), function);
	}

	protected <H extends HiggsClientConnection<String, BosonMessage, BosonMessage, ByteBuf>> H newClientRequest(HiggsClient<String, BosonMessage, BosonMessage, ByteBuf> client, String serviceName, String host, int port, boolean decompress, boolean useSSL, HiggsInitializer initializer) {
		return (H) new BosonClientConnection(
				client, serviceName, host, port, decompress, useSSL, initializer
		);
	}
}
