package com.fillta.higgs.boson;

import com.fillta.higgs.EventProcessor;
import com.fillta.higgs.HiggsEncoderDecoderInitializer;
import com.fillta.higgs.HiggsEventHandlerProxy;
import com.fillta.higgs.boson.serialization.BosonDecoder;
import com.fillta.higgs.boson.serialization.BosonEncoder;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelInboundMessageHandlerAdapter;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class BosonInitializer extends HiggsEncoderDecoderInitializer<ByteBuf, ByteBuf> {
	EventProcessor events;

	public BosonInitializer(EventProcessor bosonServer, boolean inflate, boolean deflate, boolean ssl) {
		super(inflate, deflate, ssl);
		events = bosonServer;
	}

	@Override
	public ChannelInboundMessageHandlerAdapter handler() {
		return new HiggsEventHandlerProxy(events);
	}

	@Override
	public ByteToMessageDecoder<ByteBuf> decoder() {
		return new BosonDecoder();
	}

	@Override
	public MessageToByteEncoder<ByteBuf> encoder() {
		return new BosonEncoder();
	}
}
