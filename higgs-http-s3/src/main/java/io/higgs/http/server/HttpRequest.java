package io.higgs.http.server;

import io.higgs.http.server.params.FormFiles;
import io.higgs.http.server.params.FormParams;
import io.higgs.http.server.params.HttpCookie;
import io.higgs.http.server.params.HttpCookies;
import io.higgs.http.server.params.HttpFile;
import io.higgs.http.server.params.QueryParams;
import io.higgs.http.server.params.ResourcePath;
import io.higgs.http.server.resource.MediaType;
import io.higgs.reflect.ReflectionUtil;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.CookieDecoder;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class HttpRequest extends DefaultFullHttpRequest {
    private final ReflectionUtil reflection = new ReflectionUtil();
    private final QueryParams queryParams = new QueryParams();
    private final FormFiles files = new FormFiles();
    private final FormParams form = new FormParams();
    private final HttpCookies cookies = new HttpCookies();

    private Logger log = LoggerFactory.getLogger(getClass());
    private boolean supportedMethod = true;
    private Endpoint endpoint;
    private ResourcePath path;
    private List<MediaType> mediaTypes = new ArrayList<>();
    private boolean newSession;
    private final DateTime createdAt = new DateTime();
    private HttpCookie session;

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

    /**
     * Copy a Netty request via reflection to a Higgs request
     *
     * @param request
     */
    public HttpRequest(io.netty.handler.codec.http.HttpRequest request) {
        this(request.getProtocolVersion(), request.getMethod(), request.getUri());
        List<Field> fields = reflection.getAllFields(new ArrayList<Field>(), DefaultHttpRequest.class, 10);
        for (Field field : fields) {
            try {
                field.setAccessible(true);
                //get the field from the original request and set it in this instance
                field.set(this, field.get(request));
            } catch (Throwable t) {
                log.debug("", t);
            }
        }
    }

    /**
     * Because some custom fields depend on headers not set on construction this method
     * must be invoked after Netty populates the headers.
     */
    void init() {
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
        initSession();
    }

    public void initSession() {
        if (session == null) {
            session = getCookie(HttpServer.SID);
        }
    }

    public List<MediaType> getMediaTypes() {
        return mediaTypes;
    }

    public void setUnsupportedMethod(final boolean supported) {
        this.supportedMethod = supported;
    }

    public boolean isSupportedMethod() {
        return supportedMethod;
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

    public void setEndpoint(final Endpoint endpoint) {
        this.endpoint = endpoint;
    }

    public Endpoint getEndpoint() {
        return endpoint;
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

    public boolean hasSessionID() {
        return getCookie(HttpServer.SID) != null;
    }

    public void setCookie(final HttpCookie cookie) {
        cookies.put(cookie.getName(), cookie);
    }

    public boolean isNewSession() {
        return newSession;
    }

    public void setNewSession(final HttpCookie session) {
        this.newSession = true;
        this.session = session;
    }

    public HttpCookie getSession() {
        initSession();
        return session;
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

    public String getSessionId() {
        return getSession().getValue();
    }

    @Override
    public String toString() {
        return "HttpRequest{" +
                "newSession=" + newSession +
                ", mediaTypes=" + mediaTypes.size() +
                ", path=" + path +
                ", endpoint=" + endpoint +
                ", supportedMethod=" + supportedMethod +
                ", cookies=" + cookies.size() +
                ", form=" + form.size() +
                ", files=" + files.size() +
                ", queryParams=" + queryParams.size() +
                '}';
    }

    public DateTime getCreatedAt() {
        return createdAt;
    }
}
