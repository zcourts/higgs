package io.higgs.ws.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public interface WebSocketEventListener {
    /**
     * Invoked when a connection is established
     *
     * @param ctx available ctx
     */
    void onConnect(ChannelHandlerContext ctx);

    /**
     * Invoked when the connection is closed for whatever reason
     *
     * @param ctx   available ctx
     * @param frame If available this may contain a reason why the connection is closed. Not always provided,can be null
     */
    void onClose(ChannelHandlerContext ctx, CloseWebSocketFrame frame);

    /**
     * Invoked when a heart beat is sent to keep the connection alive, typically.
     *
     * @param ctx   available ctx
     * @param frame any data sent in the ping, clients are expected to send a
     *              {@link io.netty.handler.codec.http.websocketx.PongWebSocketFrame} in response to the ping
     */
    void onPing(ChannelHandlerContext ctx, PingWebSocketFrame frame);

    /**
     * Invoked when a message is received
     *
     * @param ctx available ctx
     * @param msg the data received
     */
    void onMessage(ChannelHandlerContext ctx, WebSocketMessage msg);

    /**
     * Invoked for any un-caught exception thrown in the pipeline
     *
     * @param ctx      available ctx
     * @param cause    the exception that caused this to be invoked
     * @param response the response received, IFF available
     */
    void onError(ChannelHandlerContext ctx, Throwable cause, FullHttpResponse response);
}
