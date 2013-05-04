package io.higgs.ws.protocol;

import io.higgs.core.ProtocolDetectorFactory;
import io.higgs.http.server.protocol.HttpProtocolConfiguration;

public class WebSocketConfiguration extends HttpProtocolConfiguration {
    private String websocketPath = "/";

    @Override
    public ProtocolDetectorFactory getProtocol() {
        //this is the method to override to provide a WebSocket detector
        return new WebSocketDetectorFactory(this);
    }

    public String getWebsocketPath() {
        return websocketPath;
    }
}
