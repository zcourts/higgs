package io.higgs.http.server;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;

/**
 * A response pusher is a generic interface used by HTTP and WebSockets to write responses back to the browser.
 * This interface allows support for PUSH technologies and WebSockets to work seamlessly by using the same interface.
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
public interface MessagePusher {

    /**
     * Push a message up to the client. If the underlying connection is a web socket then
     * a new websocket frame is written, if it is a persistent HTTP request the message is written
     * as additional data to anything previously sent.
     *
     * @param message the message to push to the client
     * @return a future which is notified when the message is successfully sent, cancelled or otherwise interrupted
     * due to failure. This includes if the underlying connection has been closed.
     */
    ChannelFuture push(Object message);

    /**
     * @return The underlying context which supports this pusher
     */
    ChannelHandlerContext ctx();
}
