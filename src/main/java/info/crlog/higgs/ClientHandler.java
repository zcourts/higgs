package info.crlog.higgs;

import io.netty.channel.*;
import io.netty.handler.ssl.SslHandler;
import java.util.HashSet;
import java.util.Set;

/**
 * Handles a client-side channel.
 */
public class ClientHandler extends SimpleChannelUpstreamHandler {

    protected boolean useSSL;
    protected Channel channel;
    protected Set<HiggsEventListener> listeners;

    public ClientHandler(boolean usessl) {
        this.useSSL = usessl;
        listeners = new HashSet<HiggsEventListener>();
    }

    public void addEventListener(HiggsEventListener l) {
        listeners.add(l);
    }

    public void removeEventListener(HiggsEventListener l) {
        listeners.remove(l);
    }

    /**
     * Get a ref. to the channel connected to the remote peer.
     *
     * @return null if not connected yet or the connected Channel
     */
    public Channel getChannel() {
        return channel;
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
            ChannelHandlerContext ctx,
            ChannelStateEvent e) throws Exception {
        if (useSSL) {
            // Get the SslHandler from the pipeline
            // which were added in SecureChatPipelineFactory.
            SslHandler sslHandler = ctx.getPipeline().get(SslHandler.class);
            // Begin handshake.
            ChannelFuture handshakeFuture = sslHandler.handshake();
            handshakeFuture.addListener(new ChannelFutureListener() {

                public void operationComplete(ChannelFuture future) throws Exception {
                    if (future.isSuccess()) {
                        // Register the channel to the global channel list
                        channel = future.getChannel();
                    } else {
                        future.getChannel().close();
                    }
                }
            });
        } else {
            channel = e.getChannel();
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
