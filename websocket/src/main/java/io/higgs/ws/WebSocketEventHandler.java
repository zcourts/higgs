package io.higgs.ws;

import io.higgs.core.InvokableMethod;
import io.higgs.ws.protocol.WebSocketConfiguration;
import io.higgs.ws.protocol.WebSocketHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.util.Queue;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public interface WebSocketEventHandler {
    void onMessage(TextWebSocketFrame frame, WebSocketHandler webSocketHandler, ChannelHandlerContext ctx,
                   Queue<InvokableMethod> methods, WebSocketConfiguration config);
}
