package com.fillta.higgs.boson;

import com.fillta.higgs.RPCServer;
import com.fillta.higgs.boson.serialization.BosonDecoder;
import com.fillta.higgs.boson.serialization.BosonEncoder;
import com.fillta.higgs.boson.serialization.v1.BosonReader;
import com.fillta.higgs.boson.serialization.v1.BosonWriter;
import com.fillta.higgs.events.ChannelMessage;
import com.google.common.base.Optional;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class BosonServer extends RPCServer<BosonMessage, BosonMessage, ByteBuf> {
	protected boolean compression;
	protected boolean ssl;

	public BosonServer(int port) {
		this(port, false);
		setEnableProtocolSniffing(false);
	}

	public BosonServer(int port, boolean compression) {
		this(port, compression, false);
	}

	public BosonServer(int port, boolean compression, boolean ssl) {
		super(port);
		this.compression = compression;
		this.ssl = ssl;
	}

	public Object[] getArguments(final Class<?>[] argTypes, ChannelMessage<BosonMessage> request) {
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
	public ByteBuf serialize(final Channel ctx, final BosonMessage msg) {
		return new BosonWriter(msg).serialize();
	}

	@Override
	public BosonMessage deserialize(final ChannelHandlerContext ctx, final ByteBuf msg) {
		return new BosonReader(msg).deSerialize();
	}

	@Override
	public String getTopic(final BosonMessage msg) {
		return msg.method;
	}

	@Override
	protected boolean setupPipeline(final ChannelPipeline pipeline) {
		pipeline.addLast("decoder", new BosonDecoder());
		pipeline.addLast("encoder", new BosonEncoder());
		return true;//auto add handler
	}
}
