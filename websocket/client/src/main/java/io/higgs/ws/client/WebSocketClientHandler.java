package io.higgs.ws.client;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.GenericFutureListener;
import org.apache.log4j.Logger;

import java.util.Set;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class WebSocketClientHandler extends SimpleChannelInboundHandler<Object> {

    protected final Set<WebSocketEventListener> listensers;
    private final WebSocketClientHandshaker handshaker;
    private final boolean autoPong;
    private ChannelPromise handshakeFuture;
    private FullHttpResponse response;
    private ChannelHandlerContext ctx;

    public WebSocketClientHandler(WebSocketClientHandshaker handshaker, Set<WebSocketEventListener> listeners,
                                  boolean autoPong) {
        this.handshaker = handshaker;
        this.listensers = listeners;
        this.autoPong = autoPong;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        handshakeFuture = ctx.newPromise();
    }

    @Override
    public void channelActive(ChannelHandlerContext c) throws Exception {
        //if channelActive is called then we're not behind a proxy otherwise the proxy's connect handler would have
        // had it's channel#ACtive called
        doHandshake(c);
    }

    @Override
    public void channelInactive(ChannelHandlerContext c) throws Exception {
        for (WebSocketEventListener l : listensers) {
            l.onClose(c, null);
        }
    }

    @Override
    public void channelRead0(final ChannelHandlerContext c, Object msg) throws Exception {
        Channel ch = ctx.channel();
        if (response == null) {
            if (msg instanceof FullHttpResponse) {
                response = (FullHttpResponse) msg;
            } else {
                response = new WSResponse((DefaultHttpResponse) msg, ctx.alloc().buffer());
            }
            if (completeHandshake(ctx)) {
                return;
            }
        }
        if (msg instanceof LastHttpContent) {
            return;
        }

        if (!(msg instanceof WebSocketFrame)) {
            throw new Exception("Unexpected FullHttpResponse (getStatus=" + response.getStatus() + ", " +
                    "content=" + response.content().toString(CharsetUtil.UTF_8) + ')');
        }

        final WebSocketFrame frame = (WebSocketFrame) msg;
        if (frame instanceof TextWebSocketFrame) {
            for (WebSocketEventListener l : listensers) {
                l.onMessage(c, new WebSocketMessage(((TextWebSocketFrame) frame).text()));
            }
        } else if (frame instanceof PingWebSocketFrame) {
            if (autoPong) {
                ctx.writeAndFlush(new PongWebSocketFrame(frame.content().copy()));
            }
            for (WebSocketEventListener l : listensers) {
                l.onPing(c, (PingWebSocketFrame) frame.copy());
            }
        } else if (frame instanceof PongWebSocketFrame) {
            Logger.getLogger(getClass()).warn(
                    String.format("WebSocketClient received a PongWebSocketFrame, that shouldn't happen! Data : %s",
                            frame.content().toString(CharsetUtil.UTF_8))
            );
        } else if (frame instanceof CloseWebSocketFrame) {
            ch.close();
            for (WebSocketEventListener l : listensers) {
                l.onClose(c, (CloseWebSocketFrame) frame.copy());
            }
        }
    }

    protected void doHandshake(final ChannelHandlerContext ctx) {
        this.ctx = ctx;
        handshaker.handshake(ctx.channel()).addListener(new GenericFutureListener<ChannelFuture>() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (!future.isSuccess()) {
                    exceptionCaught(ctx, future.cause());
                }
            }
        });
    }

    protected boolean completeHandshake(ChannelHandlerContext ctx) {
        if (!handshaker.isHandshakeComplete()) {
            if (response != null && response.getStatus().code() > 299) {
                for (WebSocketEventListener l : listensers) {
                    l.onError(ctx, null, response);
                }
                return true;
            }
            handshaker.finishHandshake(ctx.channel(), response);
            handshakeFuture.setSuccess();
            for (WebSocketEventListener l : listensers) {
                l.onConnect(ctx);
            }
            return true;
        }
        return false;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (!handshakeFuture.isDone()) {
            handshakeFuture.setFailure(cause);
        }
        for (WebSocketEventListener l : listensers) {
            l.onError(ctx, cause, response);
        }
        ctx.close();
    }

    private static class WSResponse extends DefaultFullHttpResponse implements FullHttpResponse {

        protected final DefaultHttpResponse response;

        public WSResponse(DefaultHttpResponse msg, ByteBuf content) {
            super(msg.getProtocolVersion(), msg.getStatus(), content);
            this.response = msg;
        }

        public HttpHeaders headers() {
            return response.headers();
        }

        public HttpResponseStatus getStatus() {
            return response.getStatus();
        }

        public DecoderResult getDecoderResult() {
            return response.getDecoderResult();
        }
    }
}
