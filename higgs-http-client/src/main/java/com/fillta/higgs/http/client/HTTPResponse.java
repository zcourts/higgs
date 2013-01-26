package com.fillta.higgs.http.client;

import com.fillta.functional.Function1;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class HTTPResponse {
    protected static final Charset utf8 = Charset.forName("UTF-8");
    protected String requestID;
    protected boolean chunked;
    protected HttpVersion protocolVersion;
    protected HttpResponseStatus status;
    protected Map<String, List<String>> headers = new HashMap();
    protected ByteBuf buffer = Unpooled.buffer();
    protected ByteBufInputStream data = new ByteBufInputStream(buffer);
    protected boolean streamEnded;
    protected Function1<String> lineReader;
    protected Function1<String> entireBufferReader;

    public String getRequestID() {
        return requestID;
    }

    /**
     * When an HTTP response is received its entire contents are not always available.
     * The response is made available from an {@link InputStream} so that it can be read in parts.
     * This method will return false until all the contents of the HTTP response is received, at which time
     * it'll return true.
     *
     * @return false until the entire contents of the HTTP response is available
     */
    public boolean isStreamEnded() {
        return streamEnded;
    }

    void streamEnded() {
        streamEnded = true;
        fireStreamEnded();
    }

    private void fireStreamEnded() {
        if (entireBufferReader != null) {
            entireBufferReader.apply(buffer.toString(0, buffer.writerIndex(), utf8));
            //read the entire stream
            buffer.readerIndex(buffer.writerIndex());
        }
    }

    /**
     * Writes the data from the given buffer to this response's underlying buffer...
     *
     * @param data the data to make available
     */
    public void write(ByteBuf data) {
        buffer.writeBytes(data);
        fireReadLine();
    }

    private void fireReadLine() {
        if (lineReader != null) {
            String line;
            try {
                //while ((line = data.readLine()) != null)
                while ((line = data.readLine()) != null) {
                    lineReader.apply(line);
                }
            } catch (IOException e) {
                //hmmm, what to do...?
            }
        }
    }

    public ByteBuf getBuffer() {
        return buffer;
    }

    public ByteBufInputStream getData() {
        return data;
    }

    public InputStream getInputStream() {
        return data;
    }

    /**
     * Sets a function which is invoked for every line available in the response.
     * If this response is chunked or contains multiple lines the function will be invoked multiple times, once
     * for each line available. Each time the argument to the function will be a non-null entire line of input
     * NOTE: setting this and using {@link #readAll(Function1)} will cause both functions to be executed at
     * the time relevant to them.
     *
     * @param lineReader the callback
     */
    public void readLine(Function1<String> lineReader) {
        this.lineReader = lineReader;
    }

    /**
     * Sets a function which is invoked once the entire HTTP response is received.
     * WARNING: If a response is chunked and a lot of data is streamed in chunks this can result in
     * excessive memory usage. For example, if reading an HTTP stream such as Twitter's "garden hose"
     * it is not wise to use this instead {@link #readLine(Function1)} is more appropriate.
     * On the other hand, if you're sure this is just a "normal" response with a fixed,reasonable length
     * this this is the method to use.
     * NOTE: setting this and using {@link #readLine(Function1)} will cause both functions to be executed at
     * the time relevant to them.
     *
     * @param bufferedReader a function that is invoked once all the contents of a response are received
     */
    public void readAll(Function1<String> bufferedReader) {
        entireBufferReader = bufferedReader;
    }

    public void setRequestID(String requestID) {
        this.requestID = requestID;
    }

    /**
     * @return True if the transfer encoding of the HTTP response is chunked.
     */
    public boolean isChunked() {
        return chunked;
    }

    public void setChunkedTransferEncoding(final boolean isChunked) {
        this.chunked = isChunked;
    }

    public HttpVersion getProtocolVersion() {
        return protocolVersion;
    }

    public void setProtocolVersion(final HttpVersion protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    public int status() {
        return status.code();
    }

    public HttpResponseStatus getStatus() {
        return status;
    }

    public void setStatus(final HttpResponseStatus status) {
        this.status = status;
    }

    public String toString() {
        return String.format("%s\n %s\n %s\n %s\n %s\n", status, chunked, protocolVersion, headers,
                buffer.toString(utf8));
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    /**
     * Adds a value for the given header. If the header doesn't already exist it is created.
     * Invoking multiple times with the same name creates a single header with multiple values
     *
     * @param name  the name of the header to put
     * @param value the value to add to the header
     */
    public void putHeader(final String name, final String value) {
        List<String> values = headers.get(name);
        if (values == null) {
            values = new ArrayList<>();
            headers.put(name, values);
        }
        values.add(value);
    }
}
