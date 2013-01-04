package com.fillta.higgs.ws.flash;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class Decoder extends ByteToMessageDecoder<ByteBuf> {
	protected ByteBuf decode(final ChannelHandlerContext context, final ByteBuf buf) throws Exception {
		//first 3 bytes is header
		//next 4 bytes is the message size
		//everything after is the string payload
		if (buf.readableBytes() < 4)
			return null;
//		ByteBuf header = buf.readBytes(3); //get header and increase reader index by 3
		int size = buf.readBytes(4).readInt();//get the message size
		if (buf.readableBytes() < size) //if the entire message isn't available yet, wait...
			return null;
		//read the entire message, without the header or size and return it
		return buf.readBytes(size);
	}
}
