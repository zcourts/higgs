package io.higgs.ws.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public interface WebSocketServerEventListener extends WebSocketEventListener {
    /**
     * Invoked when a {@link io.netty.handler.codec.http.websocketx.PingWebSocketFrame} is received
     *
     * @param ctx   available ctx
     * @param frame the data sent back in response to a ping
     */
    void onPong(ChannelHandlerContext ctx, PongWebSocketFrame frame);
}
