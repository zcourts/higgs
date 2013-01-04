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
 * We have to override {@link FlashSocketProtocolDetector} because the protocol detector requires only 3 bytes
 * where as to detect the flash policy file request 23 bytes are required
 * + the check is different...
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class FlashSocketPolicyDetector extends FlashSocketProtocolDetector{
	public FlashSocketPolicyDetector(EventProcessor events, final FlashPolicyFile policy) {
		super(events, policy);
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

	public int bytesRequired() {
		return 23;
	}
}
