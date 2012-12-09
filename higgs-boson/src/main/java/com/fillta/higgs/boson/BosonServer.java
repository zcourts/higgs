package com.fillta.higgs.boson;

import com.fillta.higgs.MessageConverter;
import com.fillta.higgs.MessageTopicFactory;
import com.fillta.higgs.RPCServer;
import com.fillta.higgs.boson.serialization.v1.BosonReader;
import com.fillta.higgs.boson.serialization.v1.BosonWriter;
import com.fillta.higgs.events.ChannelMessage;
import com.google.common.base.Optional;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class BosonServer extends RPCServer<BosonMessage, BosonMessage, ByteBuf> {
	protected boolean compression;
	protected boolean ssl;

	public BosonServer(int port) {
		this(port, false);
	}

	public BosonServer(int port, boolean compression) {
		this(port, compression, false);
	}

	public BosonServer(int port, boolean compression, boolean ssl) {
		super(port);
		this.compression = compression;
		this.ssl = ssl;
	}

	public Object[] getArguments(ChannelMessage<BosonMessage> request) {
		return request.message.arguments;
	}

	protected BosonMessage newResponse(String methodName, ChannelMessage<BosonMessage> request,
	                                   Optional<Object> returns, Optional<Throwable> error) {
		BosonMessage msg = new BosonMessage();
		msg.method = request.message.callback;
		Object[] args = new Object[2];
		args[0] = null;
		if (returns.isPresent()) {
			args[0] = returns.get();
		}
		args[1] = null;
		if (error.isPresent()) {
			args[1] = error.get();
		}
		msg.arguments = args;
		return msg;
	}

	@Override
	public ChannelInitializer<SocketChannel> initializer() {
		return new BosonInitializer(this, compression, compression, ssl);
	}


	@Override
	public MessageConverter<BosonMessage, BosonMessage, ByteBuf> serializer() {
		return new MessageConverter<BosonMessage, BosonMessage, ByteBuf>() {
			public ByteBuf serialize(Channel chan, BosonMessage msg) {
				return new BosonWriter(msg).serialize();
			}

			public BosonMessage deserialize(ChannelHandlerContext ctx, ByteBuf msg) {
				return new BosonReader(msg).deSerialize();
			}
		};
	}

	public MessageTopicFactory<String, BosonMessage> topicFactory() {
		return new MessageTopicFactory<String, BosonMessage>() {
			public String extract(BosonMessage msg) {
				return msg.method;
			}
		};
	}

}
