package io.higgs.http.client;

import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class HttpRequest extends DefaultFullHttpRequest {
    private final HttpRequestBuilder req;
    private final String id;

    /**
     * Creates a new instance.
     *
     * @param httpVersion the HTTP version of the request
     * @param method      the HTTP method of the request
     * @param uri         the URI or path of the request
     */
    public HttpRequest(HttpRequestBuilder req, HttpVersion httpVersion, HttpMethod method, String uri) {
        super(httpVersion, method, uri);
        this.req = req;
        id = req.url() + "-" + System.nanoTime();
    }

    public HttpRequestBuilder getReq() {
        return req;
    }

    public String getId() {
        return id;
    }

    public void setHeader(final String name, final String value) {
        headers().setHeader(this, name, value);
    }

    public void setHeader(final String name, final Object value) {
        headers().setHeader(this, name, value);
    }
}
