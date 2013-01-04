package com.fillta.higgs.ws.flash;

import com.fillta.higgs.ws.FlashPolicyFile;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.nio.charset.Charset;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class FlashPolicyDecoder extends ByteToMessageDecoder<ByteBuf> {
	private final FlashPolicyFile flashPolicy;

	public FlashPolicyDecoder(final FlashPolicyFile policy) {
		flashPolicy = policy;
	}

	protected ByteBuf decode(final ChannelHandlerContext context, final ByteBuf buf) throws Exception {
		//the first decode request will be the flash policy file request.
		// this is 23 bytes. read and return, handled specially
		if (buf.readableBytes() < 23)
			return null;
		int index = buf.readerIndex();
		ByteBuf in = buf.readBytes(23);
		String request = in.toString(Charset.forName("utf-8"));
		//adobe documents it as "<policy-file-request />" with a space but in reality flash has no space
		//http://help.adobe.com/en_US/AS2LCR/Flash_10.0/help.html?content=00000471.html
		if (request.contains("<policy-file-request")) {
			//write the policy file
			context.write(flashPolicy);
			return in;
		}
		//reset reader index
		buf.readerIndex(index);
		return null;
	}
}
