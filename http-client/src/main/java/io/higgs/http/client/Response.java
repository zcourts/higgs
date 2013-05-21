package io.higgs.http.client;

import io.higgs.http.client.future.Reader;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class Response {
    private boolean chunked;
    private HttpVersion protocolVersion;
    private HttpResponseStatus status;
    private HttpHeaders headers;
    private boolean completed;

    protected Reader reader;
    protected final Request request;

    public Response(Request request, Reader reader) {
        this.reader = reader;
        this.request = request;
        reader.response(this);
    }

    public void setChunked(boolean chunked) {
        this.chunked = chunked;
    }

    public boolean isChunked() {
        return chunked;
    }

    public void setProtocolVersion(HttpVersion protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    public HttpVersion getProtocolVersion() {
        return protocolVersion;
    }

    public void setStatus(HttpResponseStatus status) {
        this.status = status;
    }

    public HttpResponseStatus getStatus() {
        return status;
    }

    public void setHeaders(HttpHeaders headers) {
        this.headers = headers;
    }

    public HttpHeaders getHeaders() {
        return headers;
    }

    public void write(ByteBuf content) {
        if (content != null) {
            reader.data(content);
        }
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
        reader.setCompleted(completed);
    }

    public boolean isCompleted() {
        return completed;
    }

    @Override
    public String toString() {
        return "Response{" +
                "chunked=" + chunked +
                ", protocolVersion=" + protocolVersion +
                ", status=" + status +
                ", headers=" + headers +
                ", completed=" + completed +
                ", reader=" + reader +
                ", \nrequest=" + request +
                '}';
    }

    /**
     * @return The request which generated this response
     */
    public Request request() {
        return request;
    }
}
