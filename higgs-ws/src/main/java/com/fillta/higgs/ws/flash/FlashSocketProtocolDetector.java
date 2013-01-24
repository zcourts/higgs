package com.fillta.higgs.ws.flash;

import com.fillta.higgs.EventProcessor;
import com.fillta.higgs.sniffing.ProtocolDetector;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class FlashSocketProtocolDetector implements ProtocolDetector {
	protected EventProcessor events;
	protected final FlashPolicyFile policy;

	public FlashSocketProtocolDetector(EventProcessor events, final FlashPolicyFile policy) {
		this.events = events;
		this.policy = policy;
	}

	public Boolean apply(final ByteBuf in) {
		int magic1 = in.getByte(in.readerIndex());
		int magic2 = in.getByte(in.readerIndex() + 1);
		int magic3 = in.getByte(in.readerIndex() + 2);
		//HFS => Higgs Flash Socket (Header)
		if (magic1 == Encoder.H && magic2 == Encoder.F && magic3 == Encoder.S) {
			//advance the reader index by the 3 bytes for the header so that the decoder doesn't need to do it
			in.readerIndex(in.readerIndex() + 3);
			return true;
		}
		return false;
	}

	public boolean setupPipeline(final ChannelHandlerContext ctx) {
		ChannelPipeline pipeline = ctx.pipeline();
		if (pipeline.get("decoder") != null)
			pipeline.remove("decoder");
		if (pipeline.get("encoder") != null)
			pipeline.remove("encoder");
		if (pipeline.get("handler") != null)
			pipeline.remove("handler");
		pipeline.addLast("decoder", new Decoder());
		pipeline.addLast("encoder", new Encoder());
		pipeline.addLast("handler", events);
		//protocol sniffer shouldn't remove itself from the pipeline
		return false;
	}

	public int bytesRequired() {
		return 3;
	}
}
