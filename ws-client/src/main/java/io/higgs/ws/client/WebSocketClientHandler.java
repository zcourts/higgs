package io.higgs.ws.client;

import io.higgs.events.Events;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.CharsetUtil;

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

    public WebSocketClientHandler(WebSocketClientHandshaker handshaker, Events events) {
        this.handshaker = handshaker;
        this.events = events;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        handshakeFuture = ctx.newPromise();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        handshaker.handshake(ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        events.emit(DISCONNECT, ctx);
    }

    @Override
    public void channelRead0(final ChannelHandlerContext ctx, Object msg) throws Exception {
        Channel ch = ctx.channel();
        if (!handshaker.isHandshakeComplete()) {
            handshaker.finishHandshake(ch, (FullHttpResponse) msg);
            handshakeFuture.setSuccess();
            events.emit(CONNECT, ctx);
            return;
        }

        if (msg instanceof FullHttpResponse) {
            FullHttpResponse response = (FullHttpResponse) msg;
            throw new Exception("Unexpected FullHttpResponse (getStatus=" + response.getStatus() + ", content="
                    + response.content().toString(CharsetUtil.UTF_8) + ')');
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
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (!handshakeFuture.isDone()) {
            handshakeFuture.setFailure(cause);
        }
        events.emit(ERROR, cause, ctx);
        ctx.close();
    }
}
