package io.higgs.http.client;

import io.higgs.core.ssl.SSLConfigFactory;
import io.higgs.core.ssl.SSLContextFactory;
import io.higgs.http.client.readers.Reader;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;

import javax.net.ssl.SSLEngine;
import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class HttpRequestBuilder {

    private static final HttpRequestBuilder instance = new HttpRequestBuilder();
    protected static String proxyHost, proxyUsername, proxyPassword;
    protected static int proxyPort = 80;
    private static EventLoopGroup group = new NioEventLoopGroup();
    protected String userAgent = "Mozilla/5.0 (compatible; HiggsBoson/0.0.1; +https://github.com/zcourts/higgs)";
    protected String charSet = "ISO-8859-1,utf-8;q=0.7,*;q=0.7";
    protected String acceptedLanguages = "en";
    protected String acceptedMimeTypes = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8";
    protected Set<Integer> redirectStatusCodes = new HashSet<>();
    protected String acceptedEncodings = HttpHeaders.Values.GZIP + ',' + HttpHeaders.Values.DEFLATE;
    protected String connectionHeader = HttpHeaders.Values.CLOSE;

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

    public HttpRequestBuilder() {
        //http://en.wikipedia.org/wiki/List_of_HTTP_status_codes#3xx_Redirection
        redirectStatusCodes.addAll(Arrays.asList(301, 302, 303, 307, 308));
    }

    /**
     * @return use a static instance of the request builder
     */
    public static HttpRequestBuilder instance() {
        return instance;
    }

    public static EventLoopGroup group() {
        return group;
    }

    public static void restart() {
        shutdown();
        group = new NioEventLoopGroup();
    }

    public static void shutdown() {
        group.shutdownGracefully();
    }

    /**
     * Checks if the given SSL protocol is supported by the current JVM
     *
     * @param protocol e.g. SSLv2Hello, SSLv3, TLSv1, TLSv1.1, TLSv1.2
     * @return true if supported, false otherwise
     * @throws java.lang.IllegalArgumentException if the given protocol is null or empty
     */
    public static boolean isSupportedSSLProtocol(String protocol) {
        if (protocol == null || protocol.isEmpty()) {
            throw new IllegalArgumentException("Protocol cannot be null or empty");
        }
        for (String v : getSupportedSSLProtocols()) {
            if (v.equalsIgnoreCase(protocol)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the list of SSL protocols supported by the current JVM
     *
     * @return e.g. [SSLv2Hello, SSLv3, TLSv1, TLSv1.1, TLSv1.2]
     */
    public static String[] getSupportedSSLProtocols() {
        SSLEngine engine = SSLContextFactory.getSSLSocket(SSLConfigFactory.sslConfiguration).createSSLEngine();
        engine.setUseClientMode(true);
        return engine.getSupportedProtocols();
    }

    public HttpRequestBuilder proxy(String host, int port) {
        return proxy(host, port, null, null);
    }

    /**
     * Sets proxy information what will be used to make all requests made using the client
     * i.e. this only needs to be set once and it will apply to all requests made after
     *
     * @param host     the proxy host
     * @param port     the proxy port
     * @param username username for the proxy
     * @param password password for the proxy
     * @return this
     */
    public HttpRequestBuilder proxy(String host, int port, String username, String password) {
        proxyHost = host;
        proxyPort = port;
        proxyUsername = username;
        proxyPassword = password;
        return this;
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

    /**
     * See {@link Reader} for handling incoming data and the default implementations
     * {@link io.higgs.http.client.readers.PageReader} which reads an entire page
     * {@link io.higgs.http.client.readers.LineReader} which reads a response line by line
     * or
     * {@link io.higgs.http.client.readers.FileReader} which reads a response and saves it to a file
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
        return new Request(this, group, uri, HttpMethod.GET, version, reader);
    }

    private void checkGroup() {
        if (group.isShuttingDown() || group.isShutdown()) {
            group = new NioEventLoopGroup();
        }
    }

    /**
     * See {@link Reader} for handling incoming data and the default implementations
     * {@link io.higgs.http.client.readers.PageReader} which reads an entire page
     * {@link io.higgs.http.client.readers.LineReader} which reads a response line by line
     * or
     * {@link io.higgs.http.client.readers.FileReader} which reads a response and saves it to a file
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
        return new POST(this, group, uri, version, reader);
    }

    public HTTPStreamingRequest streamJSON(URI uri, Reader reader) {
        checkGroup();
        return new HTTPStreamingRequest(this, group, uri, HttpVersion.HTTP_1_1, reader, HttpMethod.POST);
    }

    /**
     * See {@link Reader} for handling incoming data and the default implementations
     * {@link io.higgs.http.client.readers.PageReader} which reads an entire page
     * {@link io.higgs.http.client.readers.LineReader} which reads a response line by line
     * or
     * {@link io.higgs.http.client.readers.FileReader} which reads a response and saves it to a file
     *
     * @param uri    the URI to make the request to
     * @param reader the reader to use to process the response
     * @return the request
     */
    public JSONRequest postJSON(URI uri, Reader reader) {
        return postJSON(uri, HttpVersion.HTTP_1_1, reader);
    }

    public JSONRequest postJSON(URI uri, HttpVersion version, Reader reader) {
        checkGroup();
        return new JSONRequest(this, group, uri, version, reader, HttpMethod.POST);
    }

    public JSONRequest putJSON(URI uri, Reader reader) {
        return putJSON(uri, HttpVersion.HTTP_1_1, reader);
    }

    public JSONRequest putJSON(URI uri, HttpVersion version, Reader reader) {
        checkGroup();
        return new JSONRequest(this, group, uri, version, reader, HttpMethod.PUT);
    }

    /**
     * See {@link Reader} for handling incoming data and the default implementations
     * {@link io.higgs.http.client.readers.PageReader} which reads an entire page
     * {@link io.higgs.http.client.readers.LineReader} which reads a response line by line
     * or
     * {@link io.higgs.http.client.readers.FileReader} which reads a response and saves it to a file
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
        return new Request(this, group, uri, HttpMethod.DELETE, version, reader);
    }

    /**
     * See {@link Reader} for handling incoming data and the default implementations
     * {@link io.higgs.http.client.readers.PageReader} which reads an entire page
     * {@link io.higgs.http.client.readers.LineReader} which reads a response line by line
     * or
     * {@link io.higgs.http.client.readers.FileReader} which reads a response and saves it to a file
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
        return new Request(this, group, uri, HttpMethod.OPTIONS, version, reader);
    }

    /**
     * See {@link Reader} for handling incoming data and the default implementations
     * {@link io.higgs.http.client.readers.PageReader} which reads an entire page
     * {@link io.higgs.http.client.readers.LineReader} which reads a response line by line
     * or
     * {@link io.higgs.http.client.readers.FileReader} which reads a response and saves it to a file
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
        return new Request(this, group, uri, HttpMethod.HEAD, version, reader);
    }

    /**
     * See {@link Reader} for handling incoming data and the default implementations
     * {@link io.higgs.http.client.readers.PageReader} which reads an entire page
     * {@link io.higgs.http.client.readers.LineReader} which reads a response line by line
     * or
     * {@link io.higgs.http.client.readers.FileReader} which reads a response and saves it to a file
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
        return new Request(this, group, uri, HttpMethod.TRACE, version, reader);
    }

    /**
     * See {@link Reader} for handling incoming data and the default implementations
     * {@link io.higgs.http.client.readers.PageReader} which reads an entire page
     * {@link io.higgs.http.client.readers.LineReader} which reads a response line by line
     * or
     * {@link io.higgs.http.client.readers.FileReader} which reads a response and saves it to a file
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
        return new Request(this, group, uri, HttpMethod.PATCH, version, reader);
    }

    /**
     * See {@link Reader} for handling incoming data and the default implementations
     * {@link io.higgs.http.client.readers.PageReader} which reads an entire page
     * {@link io.higgs.http.client.readers.LineReader} which reads a response line by line
     * or
     * {@link io.higgs.http.client.readers.FileReader} which reads a response and saves it to a file
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
        return new Request(this, group, uri, HttpMethod.CONNECT, version, reader);
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
