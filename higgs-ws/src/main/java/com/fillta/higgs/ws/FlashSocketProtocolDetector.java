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
		//detect flash policy file request => Higgs Flash Socket (Header)
		String request = in.toString(Charset.forName("utf-8"));
		//adobe documents it as "<policy-file-request />" with a space but in reality flash 10 has no space
		//http://help.adobe.com/en_US/AS2LCR/Flash_10.0/help.html?content=00000471.html
		if (request.contains("<policy-file-request")) {
			return true;
		}
		return false;
	}

	public ChannelPipeline setupPipeline(final ChannelHandlerContext ctx) {
		ChannelPipeline pipeline = ctx.pipeline();
		//add the flashpolicy decoder first
		pipeline.addLast("flash-policy-decoder",new FlashPolicyDecoder(policy));
		pipeline.addLast("decoder", new Decoder());
		pipeline.addLast("encoder", new Encoder());
		pipeline.addLast("handler", new HiggsEventHandlerProxy(events));
		return pipeline;
	}

	public int bytesRequired() {
		return 23;
	}
}
