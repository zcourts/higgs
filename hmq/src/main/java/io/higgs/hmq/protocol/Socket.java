package io.higgs.hmq.protocol;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class Socket {
    private final SocketType localType;
    private final SocketType remoteType;
    private final ChannelHandlerContext ctx;

    public Socket(SocketType localType, SocketType remoteType, ChannelHandlerContext ctx) {
        this.localType = localType;
        this.remoteType = remoteType;
        this.ctx = ctx;
    }

    public SocketType localType() {
        return localType;
    }

    public SocketType remoteType() {
        return remoteType;
    }

    public Channel channel() {
        return ctx.channel();
    }
}
