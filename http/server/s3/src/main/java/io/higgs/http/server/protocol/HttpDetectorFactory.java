package io.higgs.http.server.protocol;

import io.higgs.core.ProtocolDetector;
import io.higgs.core.ProtocolDetectorFactory;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class HttpDetectorFactory implements ProtocolDetectorFactory {
    private final HttpProtocolConfiguration config;
    protected int priority;

    public HttpDetectorFactory(HttpProtocolConfiguration config) {
        this.config = config;
    }

    @Override
    public ProtocolDetector newProtocolDetector() {
        return new HttpDetector(config);
    }

    @Override
    public int setPriority(int value) {
        int old = priority;
        priority = value;
        return old;
    }

    @Override
    public int priority() {
        return priority;
    }

    @Override
    public int compareTo(ProtocolDetectorFactory that) {
        return that.priority() - this.priority();
    }
}
