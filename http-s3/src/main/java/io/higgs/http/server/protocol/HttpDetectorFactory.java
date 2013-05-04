package io.higgs.http.server.protocol;

import io.higgs.core.ProtocolDetector;
import io.higgs.core.ProtocolDetectorFactory;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class HttpDetectorFactory implements ProtocolDetectorFactory {
    private final HttpProtocolConfiguration config;

    public HttpDetectorFactory(HttpProtocolConfiguration config) {
        this.config = config;
    }

    @Override
    public ProtocolDetector newProtocolDetector() {
        return new HttpDetector(config);
    }

    @Override
    public int priority() {
        return 0;
    }

    @Override
    public int compareTo(ProtocolDetectorFactory that) {
        return that.priority() - this.priority();
    }
}
