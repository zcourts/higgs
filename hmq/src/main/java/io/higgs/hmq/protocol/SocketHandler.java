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
    private Socket socket;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
    }

    protected void setSocket(Socket socket) {
        this.socket = socket;
        System.out.println("New ZMQ socket created and handshake completed!");
        //try doing a subscription
        Frame subscribe = new Frame(Frame.Command.SUBSCRIBE, "B");
        socket.channel().writeAndFlush(subscribe);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.warn("Unexpected exception from downstream.", cause);
        ctx.close();
    }
}
