package io.higgs.hmq.protocol;

import io.netty.channel.ChannelHandlerContext;

/**
 * @author Courtney Robinson <courtney.robinson@datasift.com>
 */
public class Socket {
    private final SocketType type;
    private final int revison;
    private final ChannelHandlerContext ctx;

    public Socket(int revision, SocketType socketType, ChannelHandlerContext ctx) {
        this.revison = revision;
        this.type = socketType;
        this.ctx = ctx;
    }

    public SocketType type() {
        return type;
    }

    public int revison() {
        return revison;
    }

    public ChannelHandlerContext ctx() {
        return ctx;
    }
}
