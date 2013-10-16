package io.higgs.ws.client;

import io.higgs.core.method;
import io.higgs.events.EventMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author Courtney Robinson <courtney.robinson@datasift.com>
 */
public class WebSocketDemo {
    private final WebSocketStream stream;

    private WebSocketDemo(WebSocketStream stream) {
        this.stream = stream;
    }

    public static void main(String... args) throws URISyntaxException {
        WebSocketClient.maxFramePayloadLength = 655360 * 20;
        WebSocketStream stream = WebSocketClient.connect(new URI("ws://websocket.datasift.com/multi?username=zcourts&api_key=24cef71ef2da5c4a6e50586eed60f5d4"));
        stream.events().subscribe(new WebSocketDemo(stream));

        //via a proxy...wholly a bad idea but should work almost always ;)  - ONLY WSS will work in most cases
        //because ws is not tunneled it's up to the proxy server to decide if it'll allow a ws request
        //whereas with a wss request the proxy server doesn't know about the protocol because everything is encrypted
//        WebSocketClient client =
//                new WebSocketClient(new URI("wss://websocket.datasift.com/multi?username=zcourts&api_key="));
//        client.proxy("localhost", 3128, "user", "pass")
//                .execute();
//        client.stream().events().subscribe(new WebSocketDemo(client.stream()));
    }

    @method(WebSocketEvent.CONNECT_STR)
    public void onConnect(EventMessage message, ChannelHandlerContext ws) {
        System.out.println("Connected" + message);
        //low throughput 1f678ba99fbcad0b572011b390cf5124
        //stream.emit("{ \"action\" : \"subscribe\" , \"hash\": \"1f678ba99fbcad0b572011b390cf5124\"}");
        //high throughput 13e9347e7da32f19fcdb08e297019d2e
        stream.emit("{ \"action\" : \"subscribe\" , \"hash\": \"13e9347e7da32f19fcdb08e297019d2e\"}");
    }

    @method(WebSocketEvent.DISCONNECT_STR)
    public void onDisconnect(EventMessage message, ChannelHandlerContext ws) {
        System.out.println("DISCONNECTED:" + message);
    }

    @method(WebSocketEvent.PING_STR)
    public void onPing(EventMessage message, PingWebSocketFrame ws) {
        System.out.println("PING:" + message);
    }

    @method(WebSocketEvent.PONG_STR)
    public void onPong(EventMessage message, PingWebSocketFrame ws) {
        System.out.println("PONG" + message);
    }

    @method(WebSocketEvent.MESSAGE_STR)
    public void onMessage(EventMessage message, WebSocketMessage ws) {
        System.out.println("MESSAGE:" + ws);
    }

    @method(WebSocketEvent.ERROR_STR)
    public void onError(Throwable cause, ChannelHandlerContext ctx, EventMessage message, HttpResponse response) {
        System.out.println("Error:" + message);
        if (cause != null) {
            cause.printStackTrace();
        }
    }
}
