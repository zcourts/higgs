package com.fillta.higgs.ws;

import com.fillta.higgs.EventProcessor;
import com.fillta.higgs.HiggsEventHandlerProxy;
import com.fillta.higgs.sniffing.ProtocolDetector;
import com.fillta.higgs.ws.flash.Decoder;
import com.fillta.higgs.ws.flash.Encoder;
import com.fillta.higgs.ws.flash.FlashPolicyDecoder;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;

import java.nio.charset.Charset;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class FlashSocketProtocolDetector extends ProtocolDetector {
	private EventProcessor events;
	private final FlashPolicyFile policy;

	public FlashSocketProtocolDetector(EventProcessor events, final FlashPolicyFile policy) {
		this.events = events;
		this.policy = policy;
	}

	public Boolean apply(final ByteBuf in) {
		int magic1 = in.getUnsignedByte(in.readerIndex());
		int magic2 = in.getUnsignedByte(in.readerIndex() + 1);
		int magic3 = in.getUnsignedByte(in.readerIndex() + 2);
		//HFS => Higgs Flash Socket (Header)
		if (magic1 == 'H' && magic2 == 'F' && magic3 == 'S') {
			return true;
		}
		return false;
	}

	public ChannelPipeline setupPipeline(final ChannelHandlerContext ctx) {
		ChannelPipeline pipeline = ctx.pipeline();
		//add the flashpolicy decoder first
		pipeline.addLast("flash-policy-decoder", new FlashPolicyDecoder(policy));
		pipeline.addLast("decoder", new Decoder());
		pipeline.addLast("encoder", new Encoder());
		pipeline.addLast("handler", new HiggsEventHandlerProxy(events));
		return pipeline;
	}

	public int bytesRequired() {
		return 3;
	}
}
