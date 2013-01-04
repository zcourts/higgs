package com.fillta.higgs.ws.flash;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class Encoder extends MessageToByteEncoder<ByteBuf> {
	protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf buf) throws Exception {
		//first 3 bytes is header
		//next 4 bytes is the message size
		//everything after is the string payload
		buf.writeByte('H');
		buf.writeByte('F');
		buf.writeByte('S'); //header written
		buf.writeInt(msg.writerIndex());     //message size written
		buf.writeBytes(buf);  //data written
		ctx.flush();     //flush
	}
}
