package io.higgs.ws.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.GenericFutureListener;

import java.net.URI;
import java.util.Set;

import static io.higgs.ws.client.WebSocketClient.MAPPER;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class WebSocketStream {
    protected final ChannelFuture future;
    protected final URI uri;
    protected final Set<WebSocketEventListener> listeners;
    protected Channel channel;

    public WebSocketStream(URI uri, ChannelFuture cf, Set<WebSocketEventListener> listeners) {
        this.uri = uri;
        this.future = cf;
        this.listeners = listeners;
        cf.addListener(new GenericFutureListener<ChannelFuture>() {
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    channel = future.channel();
                }
            }
        });
    }

    public WebSocketStream subscribe(WebSocketEventListener listener) {
        listeners.add(listener);
        return this;
    }

    /**
     * Send a message to the server
     *
     * @param message the message to send
     * @return
     */
    public ChannelFuture send(String message) {
        if (channel == null || !channel.isActive()) {
            throw new IllegalStateException("Not connected");
        }
        return channel.writeAndFlush(new TextWebSocketFrame(message));
    }

    /**
     * Send a message to the server
     *
     * @param message the message to send
     * @return a future or null if an error occurred
     */
    public ChannelFuture send(Object message) {
        if (channel == null || !channel.isActive()) {
            throw new IllegalStateException("Not connected");
        }
        try {
            return channel.writeAndFlush(new TextWebSocketFrame(MAPPER.writeValueAsString(message)));
        } catch (final JsonProcessingException e) {
            return null;
        }
    }

    /**
     * @return The future obtained from the connection attempt.
     * Subscribe for notification of completion or error
     */
    public ChannelFuture connectFuture() {
        return future;
    }

    public Channel channel() {
        return channel;
    }
}
