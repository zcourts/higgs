package io.higgs.http.client;

import io.higgs.http.client.future.Reader;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;

import java.net.URI;

public class HttpRequestBuilder {

    private static EventLoopGroup group = new NioEventLoopGroup();

    private HttpRequestBuilder() {
    }

    /**
     * See {@link Reader} for handling incoming data and the default implementations
     * {@link io.higgs.http.client.future.PageReader} which reads an entire page
     * {@link io.higgs.http.client.future.LineReader} which reads a response line by line
     * or
     * {@link io.higgs.http.client.future.FileReader} which reads a response and saves it to a file
     *
     * @param uri    the URI to make the request to
     * @param reader the reader to use to process the response
     * @return the request
     */
    public static Request GET(URI uri, Reader reader) {
        return GET(uri, HttpVersion.HTTP_1_1, reader);
    }

    public static Request GET(URI uri, HttpVersion version, Reader reader) {
        checkGroup();
        return new Request(group, uri, HttpMethod.GET, version, reader);
    }

    /**
     * See {@link Reader} for handling incoming data and the default implementations
     * {@link io.higgs.http.client.future.PageReader} which reads an entire page
     * {@link io.higgs.http.client.future.LineReader} which reads a response line by line
     * or
     * {@link io.higgs.http.client.future.FileReader} which reads a response and saves it to a file
     *
     * @param uri    the URI to make the request to
     * @param reader the reader to use to process the response
     * @return the request
     */
    public static POST POST(URI uri, Reader reader) {
        return POST(uri, HttpVersion.HTTP_1_1, reader);
    }

    public static POST POST(URI uri, HttpVersion version, Reader reader) {
        checkGroup();
        return new POST(group, uri, version, reader);
    }

    /**
     * See {@link Reader} for handling incoming data and the default implementations
     * {@link io.higgs.http.client.future.PageReader} which reads an entire page
     * {@link io.higgs.http.client.future.LineReader} which reads a response line by line
     * or
     * {@link io.higgs.http.client.future.FileReader} which reads a response and saves it to a file
     *
     * @param uri    the URI to make the request to
     * @param reader the reader to use to process the response
     * @return the request
     */
    public static Request DELETE(URI uri, Reader reader) {
        return DELETE(uri, HttpVersion.HTTP_1_1, reader);
    }

    public static Request DELETE(URI uri, HttpVersion version, Reader reader) {
        checkGroup();
        return new Request(group, uri, HttpMethod.DELETE, version, reader);
    }

    /**
     * See {@link Reader} for handling incoming data and the default implementations
     * {@link io.higgs.http.client.future.PageReader} which reads an entire page
     * {@link io.higgs.http.client.future.LineReader} which reads a response line by line
     * or
     * {@link io.higgs.http.client.future.FileReader} which reads a response and saves it to a file
     *
     * @param uri    the URI to make the request to
     * @param reader the reader to use to process the response
     * @return the request
     */
    public static Request OPTIONS(URI uri, Reader reader) {
        return OPTIONS(uri, HttpVersion.HTTP_1_1, reader);
    }

    public static Request OPTIONS(URI uri, HttpVersion version, Reader reader) {
        checkGroup();
        return new Request(group, uri, HttpMethod.OPTIONS, version, reader);
    }

    /**
     * See {@link Reader} for handling incoming data and the default implementations
     * {@link io.higgs.http.client.future.PageReader} which reads an entire page
     * {@link io.higgs.http.client.future.LineReader} which reads a response line by line
     * or
     * {@link io.higgs.http.client.future.FileReader} which reads a response and saves it to a file
     *
     * @param uri    the URI to make the request to
     * @param reader the reader to use to process the response
     * @return the request
     */
    public static Request HEAD(URI uri, Reader reader) {
        return HEAD(uri, HttpVersion.HTTP_1_1, reader);
    }

    public static Request HEAD(URI uri, HttpVersion version, Reader reader) {
        checkGroup();
        return new Request(group, uri, HttpMethod.HEAD, version, reader);
    }

    /**
     * See {@link Reader} for handling incoming data and the default implementations
     * {@link io.higgs.http.client.future.PageReader} which reads an entire page
     * {@link io.higgs.http.client.future.LineReader} which reads a response line by line
     * or
     * {@link io.higgs.http.client.future.FileReader} which reads a response and saves it to a file
     *
     * @param uri    the URI to make the request to
     * @param reader the reader to use to process the response
     * @return the request
     */
    public static Request TRACE(URI uri, Reader reader) {
        return TRACE(uri, HttpVersion.HTTP_1_1, reader);
    }

    public static Request TRACE(URI uri, HttpVersion version, Reader reader) {
        checkGroup();
        return new Request(group, uri, HttpMethod.TRACE, version, reader);
    }

    /**
     * See {@link Reader} for handling incoming data and the default implementations
     * {@link io.higgs.http.client.future.PageReader} which reads an entire page
     * {@link io.higgs.http.client.future.LineReader} which reads a response line by line
     * or
     * {@link io.higgs.http.client.future.FileReader} which reads a response and saves it to a file
     *
     * @param uri    the URI to make the request to
     * @param reader the reader to use to process the response
     * @return the request
     */
    public static Request PATCH(URI uri, Reader reader) {
        return PATCH(uri, HttpVersion.HTTP_1_1, reader);
    }

    public static Request PATCH(URI uri, HttpVersion version, Reader reader) {
        checkGroup();
        return new Request(group, uri, HttpMethod.PATCH, version, reader);
    }

    /**
     * See {@link Reader} for handling incoming data and the default implementations
     * {@link io.higgs.http.client.future.PageReader} which reads an entire page
     * {@link io.higgs.http.client.future.LineReader} which reads a response line by line
     * or
     * {@link io.higgs.http.client.future.FileReader} which reads a response and saves it to a file
     *
     * @param uri    the URI to make the request to
     * @param reader the reader to use to process the response
     * @return the request
     */
    public static Request CONNECT(URI uri, Reader reader) {
        return CONNECT(uri, HttpVersion.HTTP_1_1, reader);
    }

    public static Request CONNECT(URI uri, HttpVersion version, Reader reader) {
        checkGroup();
        return new Request(group, uri, HttpMethod.CONNECT, version, reader);
    }

    private static void checkGroup() {
        if (group.isShuttingDown() || group.isShutdown()) {
            group = new NioEventLoopGroup();
        }
    }

    public static void shutdown() {
        group.shutdownGracefully();
    }
}
