package info.crlog.higgs;

import io.netty.channel.*;

/**
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
public interface HiggsEventListener {

    public void onMessage(ChannelHandlerContext ctx, MessageEvent e);

    public void onException(ChannelHandlerContext ctx, ExceptionEvent e);

    public void onDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e);

    public void onConnected(ChannelHandlerContext ctx, ChannelStateEvent e);

    public void onHandleUpstream(ChannelHandlerContext ctx, ChannelEvent e);
}
