package com.fillta.higgs.ws.flash;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundMessageHandlerAdapter;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class DiscardHandler extends ChannelInboundMessageHandlerAdapter<ByteBuf> {
	protected void messageReceived(final ChannelHandlerContext context, final ByteBuf buf) throws Exception {
		LoggerFactory.getLogger(getClass())
				.debug("Discarding flash policy request:" + buf.toString(Charset.forName("utf-8")));
	}
}
