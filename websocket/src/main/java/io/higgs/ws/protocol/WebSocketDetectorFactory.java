package io.higgs.ws.protocol;

import io.higgs.core.ProtocolDetector;
import io.higgs.http.server.protocol.HttpDetectorFactory;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class WebSocketDetectorFactory extends HttpDetectorFactory {
    private final WebSocketConfiguration config;

    public WebSocketDetectorFactory(WebSocketConfiguration config) {
        super(config);
        this.config = config;
    }

    @Override
    public ProtocolDetector newProtocolDetector() {
        return new WebSocketDetector(config);
    }

    @Override
    public int priority() {
        return 1;
    }

}
