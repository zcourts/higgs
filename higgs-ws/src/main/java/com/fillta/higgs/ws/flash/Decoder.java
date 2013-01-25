package com.fillta.higgs.ws.flash;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class Decoder extends ByteToMessageDecoder {
    protected TextWebSocketFrame decode(final ChannelHandlerContext context, final ByteBuf buf) throws Exception {
        //next 4 bytes is the message size
        //everything after is the string payload
        if (buf.readableBytes() < 4) {
            return null;
        }
        int size = buf.readBytes(4).readInt(); //get the message size
        if (buf.readableBytes() < size) //if the entire message isn't available yet, wait...
        {
            return null;
        }
        //read the entire message, without the header or size and return it a text web socket frame from the data.
        //seeing as WebSocketServer would like to think TextWebSocketFrame is the only thing that exists an all...
        return new TextWebSocketFrame(buf.readBytes(size));
    }
}
