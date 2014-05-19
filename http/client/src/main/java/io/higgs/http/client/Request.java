package io.higgs.http.client;

import io.higgs.core.StaticUtil;
import io.higgs.http.client.readers.Reader;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
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
import io.netty.util.concurrent.GenericFutureListener;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static javax.xml.bind.DatatypeConverter.printBase64Binary;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class Request {
    public static final Charset UTF8 = Charset.forName("UTF-8");
    protected final Response response;
    protected final Map<String, Object> queryParams = new HashMap<>();
    protected final FutureResponse future;
    protected final EventLoopGroup group;
    protected final HttpMethod method;
    protected HttpRequest request;
    protected URI uri;
    protected Channel channel;
    protected String userAgent = "Mozilla/5.0 (compatible; HiggsBoson/0.0.1; +https://github.com/zcourts/higgs)";
    protected List<Cookie> cookies = new ArrayList<>();
    protected String proxyHost = HttpRequestBuilder.proxyHost, proxyUser = HttpRequestBuilder.proxyUsername,
            proxyPass = HttpRequestBuilder.proxyPassword;
    protected int proxyPort = HttpRequestBuilder.proxyPort;
    protected HttpVersion version;
    protected Set<Integer> redirectStatusCodes = new HashSet<>();
    protected URI originalUri;
    protected DefaultFullHttpRequest proxyRequest;
    protected ChannelFuture connectFuture;
    protected boolean useSSL;
    protected boolean tunneling;
    private String[] sslProtocols;

    public Request(HttpRequestBuilder builder, EventLoopGroup group, URI uri, HttpMethod method, HttpVersion version,
                   Reader responseReader) {
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
        future = new FutureResponse(group, response);
        for (int code : redirectStatusCodes) {
            redirectOn(code);
        }
        //once Netty request is created, set default headers on it
        headers().set(HttpHeaders.Names.CONNECTION, builder.connectionHeader);
        headers().set(HttpHeaders.Names.ACCEPT_ENCODING, builder.acceptedEncodings);
        headers().set(HttpHeaders.Names.ACCEPT_CHARSET, builder.charSet);
        headers().set(HttpHeaders.Names.ACCEPT_LANGUAGE, builder.acceptedLanguages);
        headers().set(HttpHeaders.Names.USER_AGENT, builder.userAgent);
        headers().set(HttpHeaders.Names.ACCEPT, builder.acceptedMimeTypes);
    }

    protected void newNettyRequest(URI uri, HttpMethod method, HttpVersion version) {
        request = new DefaultFullHttpRequest(version, method, uri.getRawPath());
        headers().set(HttpHeaders.Names.REFERER, originalUri == null ? uri.toString() : originalUri.toString());
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
     * It doesn't mean the entire contents of the response has been received, just that it's started.
     */
    public FutureResponse execute() {
        String scheme = getScheme();
        String host = getHost();
        int port = uri.getPort();
        if (port == -1) {
            port = getPort(scheme);
            try {
                //use the newly inferred port
                uri = new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), port, uri.getPath(), uri.getQuery(),
                        uri.getFragment());
            } catch (URISyntaxException e) {
                System.err.println(e.getMessage());
            }
        }
        boolean ssl = isSSLScheme(scheme);

        headers().set(HttpHeaders.Names.HOST, host);
        useSSL = ssl;
        try {
            //configure before proxy since proxy settings may affect the final config
            configure();
            if (isProxyEnabled()) {
                host = proxyHost;
                port = proxyPort;
                configureProxy(ssl);
                if (tunneling) {
                    //SSL is always false when tunneling because even secure connections must first make an
                    //unsecured CONNECT request to the proxy. Only once this initial connection is established should
                    // the SSL handlers be added and any further traffic is then encrypted
                    useSSL = false;
                }
            }
            Bootstrap bootstrap = new Bootstrap();
            bootstrap
                    .group(group)
                    .channel(NioSocketChannel.class)
                    .handler(newInitializer());
            connect(host, port, bootstrap);
            channel = connectFuture.channel();
            connectFuture.addListener(new GenericFutureListener<ChannelFuture>() {
                @Override
                public void operationComplete(ChannelFuture f) throws Exception {
                    if (f.isSuccess()) {
                        makeTheRequest();
                    } else {
                        future.setFailure(f.cause());
                    }
                }
            });
        } catch (Throwable e) {
            future.setFailure(e);
        }
        return future;
    }

    public Channel getChannel() {
        return channel;
    }

    protected String getHost() {
        return uri.getHost() == null ? "localhost" : uri.getHost();
    }

    protected String getScheme() {
        return uri.getScheme() == null ? "http" : uri.getScheme();
    }

    protected boolean isSSLScheme(String scheme) {
        return "https".equalsIgnoreCase(scheme);
    }

    protected int getPort(String scheme) {
        if (isSSLScheme(scheme)) {
            return 443;
        }
        return 80;
    }

    protected ChannelFuture connect(String host, int port, Bootstrap bootstrap) {
        connectFuture = bootstrap.connect(host, port);
        return connectFuture;
    }

    protected ChannelHandler newInitializer() {
        ConnectHandler.InitFactory factory = new ConnectHandler.InitFactory() {
            @Override
            public ClientIntializer newInstance(boolean ssl, SimpleChannelInboundHandler<Object>
                    handler, ConnectHandler h) {
                return new ClientIntializer(ssl, handler, h, sslProtocols);
            }
        };
        return new ClientIntializer(useSSL, newInboundHandler(),
                //if proxy request exists then initializer should add it instead of the normal handler
                isProxyEnabled() && proxyRequest != null ?
                        new ConnectHandler(tunneling, request, newInboundHandler(), factory) : null, sslProtocols
        );
    }

    protected SimpleChannelInboundHandler<Object> newInboundHandler() {
        return new ClientHandler(response, future);
    }

    protected ChannelFuture makeTheRequest() {
        if (isProxyEnabled() && proxyRequest != null) {
            return StaticUtil.write(channel, proxyRequest);
        } else {
            return StaticUtil.write(channel, request);
        }
    }

    protected void configureProxy(boolean ssl) {
        //http://tools.ietf.org/html/rfc2817#section-5.2 - authority and host required
        String authority = uri.getHost() + ":" + uri.getPort();
        //if we're making an SSL connection or using any method other than GET or POST then request a tunnel
        if (ssl || tunneling || !(HttpMethod.GET.equals(method) || HttpMethod.POST.equals(method))) {
            proxyRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.CONNECT, authority);
            proxyRequest.headers().set(HttpHeaders.Names.HOST, authority);
            proxyRequest.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
            proxyRequest.headers().set("Proxy-Connection", HttpHeaders.Values.KEEP_ALIVE);
            tunneling = true;
        } else {
            String proxyURL = getProxyPath();
            request.setUri(proxyURL);
            request.headers().set(HttpHeaders.Names.HOST, uri.getHost() == null ? "localhost" : uri.getHost());
        }
        //provide authorization if configured
        if (proxyUser != null && !proxyUser.isEmpty()) {
            String encoded = printBase64Binary((proxyUser + ":" + proxyPass).getBytes(UTF8));
            String auth = "Basic " + encoded;
            (proxyRequest == null ? request : proxyRequest).headers().set(HttpHeaders.Names.PROXY_AUTHORIZATION, auth);
        }
    }

    protected String getProxyPath() {
        //proxy requests require the full URL
        //can stick http:// in because ssl connections will never use this method
        return getScheme() + "://" + uri.getHost() + request.getUri();
    }

    /**
     * Uses the proxy host to determine if proxy is enabled for this request.
     *
     * @return true if {@link #proxyHost} is not null and is not empty
     */
    public boolean isProxyEnabled() {
        return proxyHost != null && !proxyHost.isEmpty();
    }

    protected void configure() throws Exception {
        headers().set(HttpHeaders.Names.COOKIE, ClientCookieEncoder.encode(cookies));
        QueryStringEncoder encoder = new QueryStringEncoder(uri.getRawPath());
        QueryStringDecoder decoder = new QueryStringDecoder(uri);
        //add url params first
        for (Map.Entry<String, List<String>> e : decoder.parameters().entrySet()) {
            if (e.getKey() != null) {
                for (String val : e.getValue()) {
                    if (e.getKey() != null) {
                        encoder.addParam(e.getKey(), val == null ? "" : val);
                    }
                }
            }
        }
        //now add any cofnigured params overwriting existing ones
        for (Map.Entry<String, Object> e : queryParams.entrySet()) {
            if (e.getKey() != null) {
                if (e.getKey() != null) {
                    encoder.addParam(e.getKey(), e.getValue() == null ? "" : e.getValue().toString());
                }
            }
        }
        request.setUri(encoder.toString());
    }

    public Request proxy(String host, int port) {
        return proxy(host, port, null, null);
    }

    /**
     * Sets proxy information what will be used to make the request
     * These proxy settings apply only to this request, not any other made after
     *
     * @param host     the proxy host
     * @param port     the proxy port
     * @param username username for the proxy
     * @param password password for the proxy
     * @return this
     */
    public Request proxy(String host, int port, String username, String password) {
        proxyHost = host;
        proxyPort = port;
        proxyUser = username;
        proxyPass = password;
        return this;
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
        headers().set(name, value);
        return this;
    }

    /**
     * Set a header on this request
     *
     * @return this
     */
    public Request header(String name, Iterable<?> value) {
        headers().set(name, value);
        return this;
    }

    /**
     * Set a header on this request
     *
     * @return this
     */
    public Request header(String name, String value) {
        headers().set(name, value);
        return this;
    }

    public HttpHeaders headers() {
        return request.headers();
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
        if (url.startsWith(getScheme())) {
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

    public Request withSSLProtocols(String[] protocols) {
        if (protocols == null || protocols.length == 0) {
            throw new IllegalArgumentException("At least one SSL protocol must be enabled");
        }
        this.sslProtocols = protocols;
        return this;
    }
}
