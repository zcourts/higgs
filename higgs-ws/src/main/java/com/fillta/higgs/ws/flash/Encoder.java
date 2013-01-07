package com.fillta.higgs.ws.flash;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class Encoder extends MessageToByteEncoder<TextWebSocketFrame> {
	public static final int H = 72, F = 83, S = 70;

	protected void encode(ChannelHandlerContext ctx, TextWebSocketFrame out, ByteBuf buf) throws Exception {
		ByteBuf msg = out.getBinaryData();
		ByteBuf data= Unpooled.buffer();
		//next 4 bytes is the message size
		//everything after is the string payload
		data.writeInt(msg.writerIndex());     //message size written
		data.writeBytes(msg);  //data written
		buf.writeBytes(data);
		ctx.flush();     //flush
	}
}
