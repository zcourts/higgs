package io.higgs.http.client;

import io.netty.handler.codec.http.HttpResponse;

/**
 * @author Courtney Robinson <courtney.robinson@datasift.com>
 */
public class ProxyConnectionException extends RuntimeException {
    private final HttpResponse res;

    public ProxyConnectionException(String message, HttpResponse res) {
        super(message);
        this.res = res;
    }

    /**
     * @return The HTTP response returned by the proxy
     */
    public HttpResponse getProxyResponse() {
        return res;
    }
}
