package io.higgs.http.client.readers;

import io.higgs.core.func.Function2;
import io.higgs.http.client.Response;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public abstract class Reader<T> {
    protected static final Charset utf8 = Charset.forName("UTF-8");
    protected final Logger log = LoggerFactory.getLogger(Reader.class.getName());
    protected ByteBuf buffer = Unpooled.buffer();
    protected ByteBufInputStream data = new ByteBufInputStream(buffer);
    protected Set<Function2<T, Response>> functions = new HashSet<>();
    protected Response response;
    private boolean completed;

    public Reader() {
    }

    public Reader(Function2<T, Response> function) {
        if (function == null) {
            throw new IllegalArgumentException("Function cannot be null, use another constructor");
        }
        listen(function);
    }

    /**
     * @param function Adds a function to be invoked by this reader
     */
    public void listen(Function2<T, Response> function) {
        if (function != null) {
            functions.add(function);
        }
    }

    /**
     * Invoked each time a block of data is received
     *
     * @param data the data
     */
    public void data(ByteBuf data) {
        buffer.writeBytes(data);
    }

    /**
     * @param status the HTTP status the server responded with
     */
    public void onStatus(HttpResponseStatus status) {
    }

    /**
     * @param protocolVersion the HTTP version the server replied with
     */
    public void onProtocolVersion(HttpVersion protocolVersion) {
    }

    /**
     * Called as soon as the HTTP response headers are available. i.e. before the data
     *
     * @param headers the HTTP response headers
     */
    public void onHeaders(HttpHeaders headers) {
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
        if (completed) {
            done();
        }
    }

    /**
     * Called once at the end of a stream when all data is received
     */
    public abstract void done();

    public Response response() {
        return response;
    }

    public void response(Response response) {
        this.response = response;
    }

    @Override
    public String toString() {
        return "Reader{" +
                "buffer=" + buffer.toString(utf8) +
                '}';
    }
}
