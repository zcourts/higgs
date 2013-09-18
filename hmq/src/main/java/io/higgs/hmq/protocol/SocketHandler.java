package io.higgs.hmq.protocol;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Courtney Robinson <courtney.robinson@datasift.com>
 */
public class SocketHandler extends SimpleChannelInboundHandler<Object> {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("New ZMQ socket created and handshake completed!");
        //try doing a subscription
        Frame subscribe = new Frame(Frame.Command.SUBSCRIBE);
        ctx.writeAndFlush(subscribe);
    }
}
