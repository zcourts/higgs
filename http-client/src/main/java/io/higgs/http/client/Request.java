package io.higgs.http.client;

import io.higgs.core.StaticUtil;
import io.higgs.http.client.future.Reader;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.ClientCookieEncoder;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.DefaultCookie;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.QueryStringEncoder;
import io.netty.handler.codec.http.multipart.DiskFileUpload;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class Request {
    protected final Response response;
    protected final Map<String, Object> queryParams = new HashMap<>();

    protected final FutureResponse future;
    protected final EventLoopGroup group;
    private final HttpMethod method;
    private HttpVersion version;
    protected HttpRequest request;
    protected URI uri;
    protected HttpHeaders headers;
    protected Channel channel;
    protected String userAgent = "Mozilla/5.0 (compatible; HiggsBoson/0.0.1; +https://github.com/zcourts/higgs)";
    protected List<Cookie> cookies = new ArrayList<>();
    private Set<Integer> redirectStatusCodes = new HashSet<>();
    private URI originalUri;

    public Request(EventLoopGroup group, URI uri, HttpMethod method, HttpVersion version, Reader responseReader) {
        if (responseReader == null) {
            throw new IllegalArgumentException("A response reader is required, can't process the response otherwise");
        }
        response = new Response(this, responseReader);
        deleteTempFileOnExit(true);
        baseDirectory(null);
        this.uri = uri;
        this.group = group;
        this.method = method;
        this.version = version;
        //ignore uri.getRawPath, it's overwritten later in #configure()
        newNettyRequest(uri, method, version);
        future = new FutureResponse(group);
    }

    private void newNettyRequest(URI uri, HttpMethod method, HttpVersion version) {
        request = new DefaultFullHttpRequest(version, method, uri.getRawPath());
        headers = request.headers();
        headers.set(HttpHeaders.Names.REFERER, originalUri == null ? uri.toString() : originalUri.toString());
    }

    /**
     * Automatically follow redirect responses for the given status codes
     *
     * @param codes the status codes to treat as redirects
     * @return this
     */
    public Request redirectOn(int... codes) {
        for (int code : codes) {
            redirectStatusCodes.add(code);
        }
        return this;
    }

    /**
     * @return the set of status codes this request's responses should be redirected on
     */
    public Set<Integer> redirectOn() {
        return redirectStatusCodes;
    }

    /**
     * Makes the request to the server
     *
     * @return A Future which is notified when the response is acknowledged by the server.
     *         It doesn't mean the entire contents of the response has been received, just that it's started.
     */
    public FutureResponse execute() {
        String scheme = uri.getScheme() == null ? "http" : uri.getScheme();
        String host = uri.getHost() == null ? "localhost" : uri.getHost();
        int port = uri.getPort();
        if (port == -1) {
            if ("http".equalsIgnoreCase(scheme)) {
                port = 80;
            } else if ("https".equalsIgnoreCase(scheme)) {
                port = 443;
            }
        }
        boolean ssl = "https".equalsIgnoreCase(scheme);

        headers.set(HttpHeaders.Names.HOST, host);
        try {
            configure();
            Bootstrap bootstrap = new Bootstrap();
            bootstrap
                    .group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ClientIntializer(ssl, response, future));
            //connect
            channel = bootstrap.connect(host, port).sync().channel();
            makeTheRequest();
        } catch (Throwable e) {
            future.setFailure(e);
        }
        return future;
    }

    protected ChannelFuture makeTheRequest() {
        return StaticUtil.write(channel, request);
    }

    protected void configure() throws Exception {
        headers.set(HttpHeaders.Names.COOKIE, ClientCookieEncoder.encode(cookies));
        QueryStringEncoder encoder = new QueryStringEncoder(uri.getRawPath());
        QueryStringDecoder decoder = new QueryStringDecoder(uri);
        //add url params first
        for (Map.Entry<String, List<String>> e : decoder.parameters().entrySet()) {
            if (e.getKey() != null) {
                for (String val : e.getValue()) {
                    encoder.addParam(e.getKey(), val == null ? "" : val);
                }
            }
        }
        //now add any cofnigured params overwriting existing ones
        for (Map.Entry<String, Object> e : queryParams.entrySet()) {
            if (e.getKey() != null) {
                encoder.addParam(e.getKey(), e.getValue() == null ? "" : e.getValue().toString());
            }
        }
        request.setUri(new URI(encoder.toString()).getRawPath());
    }

    public Request userAgent(String agent) {
        if (agent != null) {
            userAgent = agent;
        }
        return this;
    }

    /**
     * @param baseDir system temp directory by default
     */
    public void baseDirectory(String baseDir) {
        DiskFileUpload.baseDirectory = baseDir;
    }

    /**
     * @param delete should delete  temp file on exit (on normal exit) if true
     */
    public void deleteTempFileOnExit(boolean delete) {
        DiskFileUpload.deleteOnExitTemporaryFile = delete;
    }

    /**
     * Set a header on this request
     *
     * @return this
     */
    public Request header(String name, Object value) {
        headers.set(name, value);
        return this;
    }

    /**
     * Set a header on this request
     *
     * @return this
     */
    public Request header(String name, Iterable<?> value) {
        headers.set(name, value);
        return this;
    }

    /**
     * Set a header on this request
     *
     * @return this
     */
    public Request header(String name, String value) {
        headers.set(name, value);
        return this;
    }

    public HttpHeaders headers() {
        return headers;
    }

    /**
     * Adds a query string parameter to the request
     *
     * @param name  the name of the query string
     * @param value the value
     * @return this
     */
    public Request query(String name, Object value) {
        queryParams.put(name, value);
        return this;
    }

    /**
     * Adds a cookie to this request
     *
     * @param cookie the cookie to add
     * @return this
     */
    public Request cookie(Cookie cookie) {
        if (cookie != null) {
            cookies.add(cookie);
        }
        return this;
    }

    /**
     * Adds a cookie with the given name and value
     *
     * @param name  the name
     * @param value the value
     * @return this
     */
    public Request cookie(String name, Object value) {
        if (name != null) {
            Cookie cookie = new DefaultCookie(name, value == null ? null : value.toString());
            cookies.add(cookie);
        }
        return this;
    }

    /**
     * @return List of Cookies set in this request
     */
    public List<Cookie> cookies() {
        return cookies;
    }

    public void shutdown() {
        group.shutdownGracefully();
    }

    /**
     * @return The resposne generated for this request
     */
    public Response response() {
        return response;
    }

    /**
     * @return The URI this request was first made to or NULL if the response did not result in a redirect
     */
    public URI originalUri() {
        return originalUri;
    }

    public URI url() {
        return uri;
    }

    public Request url(String url) throws URISyntaxException {
        if (url == null) {
            throw new IllegalArgumentException("NULL url provided");
        }
        originalUri = uri;
        if (url.startsWith("http")) {
            this.uri = new URI(url);
        } else {
            this.uri = uri.resolve(url);
        }
        newNettyRequest(uri, method, version);
        return this;
    }

    public HttpRequest nettyRequest() {
        return request;
    }
}
