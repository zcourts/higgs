package com.fillta.higgs.ws.flash;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class FlashPolicyEncoder extends MessageToByteEncoder<FlashPolicyFile> {
    protected void encode(ChannelHandlerContext context, FlashPolicyFile s, ByteBuf buf) throws Exception {
        buf.writeBytes(s.getBytes());
        //write NULL byte, 0x00
        buf.writeByte(0x00);
        context.flush();
    }
}
