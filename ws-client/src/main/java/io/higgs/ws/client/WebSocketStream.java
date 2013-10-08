package io.higgs.ws.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.higgs.events.Events;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.GenericFutureListener;

import java.net.URI;

import static io.higgs.ws.client.WebSocketClient.MAPPER;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class WebSocketStream {
    protected final ChannelFuture future;
    protected final URI uri;
    private final Events events;
    protected Channel channel;

    public WebSocketStream(URI uri, ChannelFuture cf, Events events) {
        this.uri = uri;
        this.future = cf;
        this.events = events;
        cf.addListener(new GenericFutureListener<ChannelFuture>() {
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    channel = future.channel();
                }
            }
        });
    }

    public Events events() {
        return events;
    }

    /**
     * Send a message to the server
     *
     * @param message the message to send
     * @return
     */
    public ChannelFuture emit(String message) {
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
    public ChannelFuture emit(Object message) {
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
     *         Subscribe for notification of completion or error
     */
    public ChannelFuture connectFuture() {
        return future;
    }

    public Channel channel() {
        return channel;
    }
}
