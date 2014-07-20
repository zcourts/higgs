package io.higgs.http.server.transformers;

import io.netty.handler.codec.http.HttpResponseStatus;

import javax.ws.rs.WebApplicationException;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class JsonResponseError extends WebApplicationException {
    public static final String EMPTY_JSON_OBJECT = "{}";
    protected Object content;

    public JsonResponseError(HttpResponseStatus status, Object content) {
        super(status.code());
        this.content = content == null ? EMPTY_JSON_OBJECT : content;
    }

    public Object getContent() {
        return content;
    }
}
