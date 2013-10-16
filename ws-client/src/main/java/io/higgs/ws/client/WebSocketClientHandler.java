package io.higgs.ws.client;

import io.higgs.events.Events;
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

import static io.higgs.ws.client.WebSocketEvent.CONNECT;
import static io.higgs.ws.client.WebSocketEvent.DISCONNECT;
import static io.higgs.ws.client.WebSocketEvent.ERROR;
import static io.higgs.ws.client.WebSocketEvent.MESSAGE;
import static io.higgs.ws.client.WebSocketEvent.PING;
import static io.higgs.ws.client.WebSocketEvent.PONG;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class WebSocketClientHandler extends SimpleChannelInboundHandler<Object> {

    protected final Events events;
    private final WebSocketClientHandshaker handshaker;
    private ChannelPromise handshakeFuture;
    private FullHttpResponse response;
    private ChannelHandlerContext ctx;

    public WebSocketClientHandler(WebSocketClientHandshaker handshaker, Events events) {
        this.handshaker = handshaker;
        this.events = events;
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
        events.emit(DISCONNECT, ctx);
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
            //emit frame as first param in case of a function since they only accept the first param emitted
            events.emit(MESSAGE, new WebSocketMessage(((TextWebSocketFrame) frame).text()), ctx);
        } else if (frame instanceof PingWebSocketFrame) {
            frame.retain();
            ctx.writeAndFlush(new PongWebSocketFrame(frame.content()));
            events.emit(PING, ctx, frame);
        } else if (frame instanceof PongWebSocketFrame) {
            events.emit(PONG, ctx, frame);
        } else if (frame instanceof CloseWebSocketFrame) {
            ch.close();
            events.emit(DISCONNECT, ctx);
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
                events.emit(ERROR, response, ctx);
                return true;
            }
            handshaker.finishHandshake(ctx.channel(), response);
            handshakeFuture.setSuccess();
            events.emit(CONNECT, ctx);
            return true;
        }
        return false;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (!handshakeFuture.isDone()) {
            handshakeFuture.setFailure(cause);
        }
        events.emit(ERROR, cause, ctx);
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
