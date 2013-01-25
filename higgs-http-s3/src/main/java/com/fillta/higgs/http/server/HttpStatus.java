package com.fillta.higgs.http.server;

import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class HttpStatus extends HttpResponseStatus {
    /**
     * Creates a new instance with the specified {@code code} and its
     * {@code reasonPhrase}.
     */
    public HttpStatus(int code, String reasonPhrase) {
        super(code, reasonPhrase);
    }
}
