package io.higgs.hmq.protocol;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class HandshakeHandler extends SimpleChannelInboundHandler<Socket> {
    private final SocketType type;
    private final SocketHandler handler;

    public HandshakeHandler(SocketType type, SocketHandler handler) {
        this.type = type;
        this.handler = handler;
    }

    public void channelActive(ChannelHandlerContext ctx) {
        ctx.channel().writeAndFlush(new Handshake(type));
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Socket socket) throws Exception {
        handler.setSocket(socket);
        //remove from pipeline, handshake is only done once
        ctx.pipeline().remove(this);
    }
}
