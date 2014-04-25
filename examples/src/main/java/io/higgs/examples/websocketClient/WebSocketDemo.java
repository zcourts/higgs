package io.higgs.examples.websocketClient;

import io.higgs.http.client.HttpRequestBuilder;
import io.higgs.ws.client.WebSocketClient;
import io.higgs.ws.client.WebSocketEventListener;
import io.higgs.ws.client.WebSocketMessage;
import io.higgs.ws.client.WebSocketStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;

/**
 * @author Courtney Robinson <courtney.robinson@datasift.com>
 */
public class WebSocketDemo implements WebSocketEventListener {
    private final WebSocketStream stream;

    private WebSocketDemo(WebSocketStream stream) {
        this.stream = stream;
    }

    public static void main(String... args) throws URISyntaxException {
        WebSocketClient.maxFramePayloadLength = 655360 * 20;
        WebSocketStream stream = WebSocketClient.connect(new URI("ws://websocket.datasift.com/multi?username=" +
                "zcourts&api_key=bc753994e3b3630556c7cf5c3f600d70"), true,
                HttpRequestBuilder.getSupportedSSLProtocols());
        stream.subscribe(new WebSocketDemo(stream));

        //via a proxy...wholly a bad idea but should work almost always ;)  - ONLY WSS will work in most cases
        //because ws is not tunneled it's up to the proxy server to decide if it'll allow a ws request
        //whereas with a wss request the proxy server doesn't know about the protocol because everything is encrypted
//        WebSocketClient client =
//                new WebSocketClient(new URI("wss://websocket.datasift.com/multi?username=zcourts&api_key="));
//        client.proxy("localhost", 3128, "user", "pass")
//                .execute();
//        client.stream().events().subscribe(new WebSocketDemo(client.stream()));
    }

    @Override
    public void onConnect(ChannelHandlerContext ctx) {
        stream.send("{ \"action\" : \"subscribe\" , \"hash\": \"13e9347e7da32f19fcdb08e297019d2e\"}");
    }

    @Override
    public void onClose(ChannelHandlerContext ctx, CloseWebSocketFrame frame) {
        System.out.println("DISCONNECTED:" + frame.reasonText());
    }

    @Override
    public void onPing(ChannelHandlerContext ctx, PingWebSocketFrame frame) {
        System.out.println("PING:" + frame.content().toString(Charset.forName("utf8")));
    }

    @Override
    public void onMessage(ChannelHandlerContext ctx, WebSocketMessage msg) {
        System.out.println("MESSAGE:" + msg.data());
    }

    @Override
    public void onError(ChannelHandlerContext ctx, Throwable cause, FullHttpResponse response) {
        System.out.println("Error:" + response);
        if (cause != null) {
            cause.printStackTrace();
        }
    }
}
