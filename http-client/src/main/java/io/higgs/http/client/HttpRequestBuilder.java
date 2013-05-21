package io.higgs.http.client;

import io.higgs.http.client.future.Reader;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class HttpRequestBuilder {

    private static EventLoopGroup group = new NioEventLoopGroup();
    private Set<Integer> redirectStatusCodes = new HashSet<>();
    protected String userAgent = "Mozilla/5.0 (compatible; HiggsBoson/0.0.1; +https://github.com/zcourts/higgs)";
    protected String charSet = "ISO-8859-1,utf-8;q=0.7,*;q=0.7";
    protected String acceptedLanguages = "en";
    protected String acceptedMimeTypes = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8";
    private String acceptedEncodings = HttpHeaders.Values.GZIP + ',' + HttpHeaders.Values.DEFLATE;
    private String connectionHeader = HttpHeaders.Values.CLOSE;
    private static final HttpRequestBuilder instance = new HttpRequestBuilder();

    public HttpRequestBuilder() {
        //http://en.wikipedia.org/wiki/List_of_HTTP_status_codes#3xx_Redirection
        redirectStatusCodes.addAll(Arrays.asList(301, 302, 303, 307, 308));
    }

    public HttpRequestBuilder(HttpRequestBuilder that) {
        this();
        redirectStatusCodes.addAll(that.redirectStatusCodes);
        userAgent = that.userAgent;
        charSet = that.charSet;
        acceptedEncodings = that.acceptedEncodings;
        acceptedLanguages = that.acceptedLanguages;
        acceptedMimeTypes = that.acceptedMimeTypes;
        connectionHeader = that.connectionHeader;
    }

    /**
     * @return use a static instance of the request builder
     */
    public static HttpRequestBuilder instance() {
        return instance;
    }

    /**
     * Automatically follow redirect responses for the given status codes
     *
     * @param codes the status codes to treat as redirects
     * @return this
     */
    public HttpRequestBuilder redirectOn(int... codes) {
        for (int code : codes) {
            redirectStatusCodes.add(code);
        }
        return this;
    }

    public Set<Integer> redirectOn() {
        return redirectStatusCodes;
    }

    public Request applyDefaults(Request request) {
        for (int code : redirectStatusCodes) {
            request.redirectOn(code);
        }
        request.headers().set(HttpHeaders.Names.CONNECTION, connectionHeader);
        request.headers().set(HttpHeaders.Names.ACCEPT_ENCODING, acceptedEncodings);
        request.headers().set(HttpHeaders.Names.ACCEPT_CHARSET, charSet);
        request.headers().set(HttpHeaders.Names.ACCEPT_LANGUAGE, acceptedLanguages);
        request.headers().set(HttpHeaders.Names.USER_AGENT, userAgent);
        request.headers().set(HttpHeaders.Names.ACCEPT, acceptedMimeTypes);
        return request;
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
    public Request GET(URI uri, Reader reader) {
        return GET(uri, HttpVersion.HTTP_1_1, reader);
    }

    public Request GET(URI uri, HttpVersion version, Reader reader) {
        checkGroup();
        return applyDefaults(new Request(group, uri, HttpMethod.GET, version, reader));
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
    public POST POST(URI uri, Reader reader) {
        return POST(uri, HttpVersion.HTTP_1_1, reader);
    }

    public POST POST(URI uri, HttpVersion version, Reader reader) {
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
    public Request DELETE(URI uri, Reader reader) {
        return DELETE(uri, HttpVersion.HTTP_1_1, reader);
    }

    public Request DELETE(URI uri, HttpVersion version, Reader reader) {
        checkGroup();
        return applyDefaults(new Request(group, uri, HttpMethod.DELETE, version, reader));
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
    public Request OPTIONS(URI uri, Reader reader) {
        return OPTIONS(uri, HttpVersion.HTTP_1_1, reader);
    }

    public Request OPTIONS(URI uri, HttpVersion version, Reader reader) {
        checkGroup();
        return applyDefaults(new Request(group, uri, HttpMethod.OPTIONS, version, reader));
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
    public Request HEAD(URI uri, Reader reader) {
        return HEAD(uri, HttpVersion.HTTP_1_1, reader);
    }

    public Request HEAD(URI uri, HttpVersion version, Reader reader) {
        checkGroup();
        return applyDefaults(new Request(group, uri, HttpMethod.HEAD, version, reader));
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
    public Request TRACE(URI uri, Reader reader) {
        return TRACE(uri, HttpVersion.HTTP_1_1, reader);
    }

    public Request TRACE(URI uri, HttpVersion version, Reader reader) {
        checkGroup();
        return applyDefaults(new Request(group, uri, HttpMethod.TRACE, version, reader));
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
    public Request PATCH(URI uri, Reader reader) {
        return PATCH(uri, HttpVersion.HTTP_1_1, reader);
    }

    public Request PATCH(URI uri, HttpVersion version, Reader reader) {
        checkGroup();
        return applyDefaults(new Request(group, uri, HttpMethod.PATCH, version, reader));
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
    public Request CONNECT(URI uri, Reader reader) {
        return CONNECT(uri, HttpVersion.HTTP_1_1, reader);
    }

    public Request CONNECT(URI uri, HttpVersion version, Reader reader) {
        checkGroup();
        return applyDefaults(new Request(group, uri, HttpMethod.CONNECT, version, reader));
    }

    private void checkGroup() {
        if (group.isShuttingDown() || group.isShutdown()) {
            group = new NioEventLoopGroup();
        }
    }

    public static void shutdown() {
        group.shutdownGracefully();
    }

    public HttpRequestBuilder acceptedLanguages(String acceptedLanguages) {
        this.acceptedLanguages = acceptedLanguages;
        return this;
    }

    public HttpRequestBuilder acceptedMimeTypes(String acceptedMimeTypes) {
        this.acceptedMimeTypes = acceptedMimeTypes;
        return this;
    }

    public HttpRequestBuilder charSet(String charSet) {
        this.charSet = charSet;
        return this;
    }

    public HttpRequestBuilder userAgent(String userAgent) {
        this.userAgent = userAgent;
        return this;
    }

    /**
     * @param connectionHeader the connection header to use on requests. by default = CLOSE
     */
    public HttpRequestBuilder connection(String connectionHeader) {
        this.connectionHeader = connectionHeader;
        return this;
    }

    /**
     * Create a new instance of {@link HttpRequestBuilder} copying all the settings from this
     * instance to the new one
     *
     * @return a new instance with the settings copied
     */
    public HttpRequestBuilder copy() {
        return new HttpRequestBuilder(this);
    }
}
