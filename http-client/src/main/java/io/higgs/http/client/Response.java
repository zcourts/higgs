package io.higgs.http.client;

import io.higgs.http.client.readers.Reader;
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
    protected boolean failed;
    protected Throwable cause;
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
        reader.onProtocolVersion(protocolVersion);
    }

    public HttpVersion getProtocolVersion() {
        return protocolVersion;
    }

    public void setStatus(HttpResponseStatus status) {
        this.status = status;
        reader.onStatus(status);
    }

    public HttpResponseStatus getStatus() {
        return status;
    }

    public void setHeaders(HttpHeaders headers) {
        this.headers = headers;
        reader.onHeaders(headers);
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

    /**
     * Mark the request as failed, this will set this response as completed
     * and notify all listeners that the request failed
     *
     * @param cause an optional exception that may have been the cause of the failure
     */
    public void markFailed(Throwable cause) {
        this.failed = true;
        this.cause = cause;
        setCompleted(true);
    }

    /**
     * @return true if the request for this response has failed for some reason
     */
    public boolean hasFailed() {
        return failed;
    }

    /**
     * @return If the request has failed, this returns the reason for the failure
     * May* be null
     */
    public Throwable failureCause() {
        return cause;
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

    /**
     * @return true if this response is the result of following a redirect
     */
    public boolean isRedirected() {
        return request.originalUri() != null;
    }
}
