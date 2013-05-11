package io.higgs.http.server;

import io.higgs.core.ResourcePath;
import io.higgs.http.server.params.FormFiles;
import io.higgs.http.server.params.FormParams;
import io.higgs.http.server.params.HttpCookie;
import io.higgs.http.server.params.HttpCookies;
import io.higgs.http.server.params.HttpFile;
import io.higgs.http.server.params.HttpSession;
import io.higgs.http.server.params.QueryParams;
import io.higgs.http.server.protocol.HttpProtocolConfiguration;
import io.higgs.http.server.resource.MediaType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.CookieDecoder;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static java.lang.Integer.parseInt;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class HttpRequest extends DefaultHttpRequest {
    private final QueryParams queryParams = new QueryParams();
    private final FormFiles files = new FormFiles();
    private final FormParams form = new FormParams();
    private final HttpCookies cookies = new HttpCookies();
    private final DateTime createdAt = new DateTime();
    private Logger log = LoggerFactory.getLogger(getClass());
    private ResourcePath path;
    private List<MediaType> mediaTypes = new ArrayList<>();
    private boolean newSession;
    private String sessionId;
    private MediaType matchedMediaType = MediaType.WILDCARD_TYPE;
    private HttpProtocolConfiguration config;
    private boolean multipart;
    private boolean chunked;
    public static final String SID = "HS3-ID";
    private static final AttributeKey<String> sessionAttr = new AttributeKey<>(SID + "-attr");
    private ByteBuf content = Unpooled.buffer(0);

    /**
     * Creates a new instance.
     *
     * @param httpVersion the HTTP version of the request
     * @param method      the HTTP method of the request
     * @param uri         the URI or path of the request
     */
    public HttpRequest(HttpVersion httpVersion, HttpMethod method, String uri) {
        super(httpVersion, method, uri);
    }

    public HttpRequest(FullHttpRequest msg) {
        this(msg.getProtocolVersion(), msg.getMethod(), msg.getUri());
        headers().add(msg.headers());
        content = msg.content();
        setDecoderResult(msg.getDecoderResult());
    }

    /**
     * Because some custom fields depend on headers not set on construction this method
     * must be invoked after Netty populates the headers.
     *
     * @param ctx
     */
    public void init(ChannelHandlerContext ctx) {
        String accept = headers().get(HttpHeaders.Names.ACCEPT);
        mediaTypes = MediaType.valueOf(accept);
        String cookiesStr = headers().get(HttpHeaders.Names.COOKIE);
        if (cookiesStr != null) {
            Set<Cookie> cookie = CookieDecoder.decode(cookiesStr);
            for (Cookie c : cookie) {
                cookies.put(c.getName(), new HttpCookie(c));
            }
        }
        QueryStringDecoder decoderQuery = new QueryStringDecoder(getUri());
        queryParams.putAll(decoderQuery.parameters());
        getSessionID(ctx);
        initSession(ctx);
    }

    public void initSession(ChannelHandlerContext ctx) {
        if (sessionId == null || config.getSessions().get(sessionId) == null) {
            if (config.getSessions().get(sessionId) == null) {
                //generate a new session ID
                SecureRandom random = new SecureRandom();
                sessionId = new BigInteger(130, random).toString(32);

                HttpCookie session = new HttpCookie(SID, sessionId);
                session.setPath(config.getServer().getConfig().session_path);
                session.setMaxAge(config.getServer().getConfig().session_max_age);
                session.setHttpOnly(config.getServer().getConfig().session_http_only);

                if (config.getServer().getConfig().session_domain != null &&
                        !config.getServer().getConfig().session_domain.isEmpty()) {
                    session.setDomain(config.getServer().getConfig().session_domain);
                }

                String sp = config.getServer().getConfig().session_ports;
                if (sp != null && !sp.isEmpty()) {
                    String[] ps = sp.split(",");
                    List<Integer> ports = new ArrayList<>(ps.length);
                    for (String p : ps) {
                        try {
                            ports.add(parseInt(p));
                        } catch (NumberFormatException nfe) {
                            log.warn(String.format("Session port config contained non-numeric value (%s)", p));
                        }
                    }
                    session.setPorts(ports);
                }
                setCookie(session); //set the session id cookie
                //need to associate session ID with the channel since multiple requests can be received
                //before the session cookie is set on the client, e.g. in keep alive requests
                Attribute<String> sessAttr = ctx.channel().attr(sessionAttr);
                sessAttr.set(sessionId);
                this.newSession = true;
                config.getSessions().put(sessionId, new HttpSession());
            }
        }
    }

    private void getSessionID(ChannelHandlerContext ctx) {
        Attribute<String> sessAttr = ctx.channel().attr(sessionAttr);
        if (sessAttr != null && sessAttr.get() != null) {
            sessionId = sessAttr.get();
        }
        HttpCookie cookie = getCookie(SID);
        if (sessionId == null && cookie != null) {
            sessionId = cookie.getValue();
        }
    }

    public List<MediaType> getMediaTypes() {
        return mediaTypes;
    }

    public boolean isGet() {
        return HttpMethod.GET.name().equalsIgnoreCase(getMethod().name());
    }

    public boolean isPost() {
        return HttpMethod.POST.name().equalsIgnoreCase(getMethod().name());
    }

    public boolean isPut() {
        return HttpMethod.PUT.name().equalsIgnoreCase(getMethod().name());
    }

    public boolean isDelete() {
        return HttpMethod.DELETE.name().equalsIgnoreCase(getMethod().name());
    }

    public boolean isHead() {
        return HttpMethod.HEAD.name().equalsIgnoreCase(getMethod().name());
    }

    public boolean isOptions() {
        return HttpMethod.OPTIONS.name().equalsIgnoreCase(getMethod().name());
    }

    public ResourcePath getPath() {
        return path;
    }

    public void setPath(final ResourcePath path) {
        this.path = path;
    }

    public HttpCookie getCookie(String name) {
        return cookies.get(name);
    }

    public HttpCookies getCookies() {
        return cookies;
    }

    /**
     * Sets a cookie with path as "/"
     *
     * @param name
     * @param value
     */
    public void setCookie(final String name, final String value) {
        HttpCookie cookie = new HttpCookie(name, value);
        cookie.setPath("/");
        setCookie(cookie);
    }

    public void setCookie(HttpCookie cookie) {
        cookies.put(cookie.getName(), cookie);
    }

    public boolean isNewSession() {
        return newSession;
    }

    public String getSessionId() {
        return sessionId;
    }

    public HttpSession getSession() {
        return config.getSessions().get(sessionId);
    }

    public void addFormField(final String name, final String value) {
        form.put(name, value);
    }

    public void addFormFile(final HttpFile file) {
        files.put(file.getParameterName(), file);
    }

    /**
     * Get the query string parameters associated with this request
     *
     * @return
     */
    public QueryParams getQueryParams() {
        return queryParams;
    }

    /**
     * Get all files uploaded with this request
     *
     * @return
     */
    public FormFiles getFormFiles() {
        return files;
    }

    /**
     * Get all form fields send with this request
     *
     * @return
     */
    public FormParams getFormParam() {
        return form;
    }

    @Override
    public String toString() {
        return "HttpRequest{" +
                "newSession=" + newSession +
                ", mediaTypes=" + mediaTypes.size() +
                ", path=" + path +
                ", cookies=" + cookies.size() +
                ", form=" + form.size() +
                ", files=" + files.size() +
                ", queryParams=" + queryParams.size() +
                '}';
    }

    public DateTime getCreatedAt() {
        return createdAt;
    }

    public void setMatchedMediaType(MediaType matchedMediaType) {
        this.matchedMediaType = matchedMediaType;
    }

    /**
     * @return The media type which matched on this request or {@link MediaType#WILDCARD} by default
     */
    public MediaType getMatchedMediaType() {
        return matchedMediaType;
    }

    public void setConfig(HttpProtocolConfiguration config) {
        this.config = config;
    }

    public HttpProtocolConfiguration getConfig() {
        return config;
    }

    public void setMultipart(boolean multipart) {
        this.multipart = multipart;
    }

    public boolean isMultipart() {
        return multipart;
    }

    public void setChunked(boolean chunked) {
        this.chunked = chunked;
    }

    public boolean isChunked() {
        return chunked;
    }

    public ByteBuf content() {
        return content;
    }
}
