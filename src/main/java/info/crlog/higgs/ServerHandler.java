package info.crlog.higgs;

import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.ssl.SslHandler;
import java.util.HashSet;
import java.util.Set;

/**
 * Handles a server-side channel.
 */
public class ServerHandler extends SimpleChannelUpstreamHandler {

    protected ChannelGroup channels;
    protected boolean useSSL;
    protected Set<HiggsEventListener> listeners;

    public ServerHandler(ChannelGroup chans, boolean usessl) {
        channels = chans;
        useSSL = usessl;
        listeners = new HashSet<HiggsEventListener>();
    }

    public void addEventListener(HiggsEventListener l) {
        listeners.add(l);
    }

    public void removeEventListener(HiggsEventListener l) {
        listeners.remove(l);
    }

    @Override
    public void handleUpstream(
            ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
        super.handleUpstream(ctx, e);
        for (HiggsEventListener l : listeners) {
            l.onHandleUpstream(ctx, e);
        }
    }

    @Override
    public void channelConnected(
            ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        if (useSSL) {
            // Get the SslHandler in the current pipeline.
            final SslHandler sslHandler = ctx.getPipeline().get(SslHandler.class);
            // Get notified when SSL handshake is done.
            //since SSL is required handshake must complete before we write
            ChannelFuture handshakeFuture = sslHandler.handshake();
            handshakeFuture.addListener(new ChannelFutureListener() {

                public void operationComplete(ChannelFuture future) throws Exception {
                    if (future.isSuccess()) {
                        // Register the channel to the global channel list
                        channels.add(future.getChannel());
                    } else {
                        future.getChannel().close();
                    }
                }
            });
        } else {
            channels.add(ctx.getChannel());
        }
        for (HiggsEventListener l : listeners) {
            l.onConnected(ctx, e);
        }
    }

    @Override
    public void channelDisconnected(
            ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        for (HiggsEventListener l : listeners) {
            l.onDisconnected(ctx, e);
        }
        // Unregister the channel from the global channel list
        // so the channel does not receive messages anymore.
        channels.remove(e.getChannel());
    }

    @Override
    public void messageReceived(
            ChannelHandlerContext ctx, MessageEvent e) {
        for (HiggsEventListener l : listeners) {
            l.onMessage(ctx, e);
        }
    }

    @Override
    public void exceptionCaught(
            ChannelHandlerContext ctx, ExceptionEvent e) {
        for (HiggsEventListener l : listeners) {
            l.onException(ctx, e);
        }
    }
}
