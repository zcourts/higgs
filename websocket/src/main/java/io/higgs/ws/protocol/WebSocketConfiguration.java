package io.higgs.ws.protocol;

import io.higgs.core.ProtocolDetectorFactory;
import io.higgs.http.server.protocol.HttpProtocolConfiguration;
import io.higgs.ws.DefaultWebSocketEventHandler;
import io.higgs.ws.WebSocketEventHandler;

public class WebSocketConfiguration extends HttpProtocolConfiguration {
    private String websocketPath = "/";
    private WebSocketEventHandler webSocketEventHandler;

    @Override
    public ProtocolDetectorFactory getProtocol() {
        //this is the method to override to provide a WebSocket detector
        return new WebSocketDetectorFactory(this);
    }

    public String getWebsocketPath() {
        return websocketPath;
    }

    public WebSocketEventHandler getWebSocketEventHandler() {
        return webSocketEventHandler == null ? new DefaultWebSocketEventHandler() : webSocketEventHandler;
    }

    public void setWebSocketEventHandler(WebSocketEventHandler webSocketEventHandler) {
        this.webSocketEventHandler = webSocketEventHandler;
    }
}
