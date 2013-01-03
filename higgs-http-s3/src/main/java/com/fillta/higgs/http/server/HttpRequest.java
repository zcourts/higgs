package com.fillta.higgs.http.server;

import com.fillta.higgs.http.server.params.*;
import com.fillta.higgs.http.server.resource.MediaType;
import com.fillta.higgs.reflect.ReflectionUtil;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class HttpRequest extends DefaultHttpRequest {
	private final ReflectionUtil reflection = new ReflectionUtil();
	private final QueryParams queryParams = new QueryParams();
	private final FormFiles files = new FormFiles();
	private final FormParams form = new FormParams();
	private final HttpCookies cookies = new HttpCookies();

	private Logger log = LoggerFactory.getLogger(getClass());
	private boolean supportedMethod = true;
	private Endpoint endpoint;
	private ResourcePath path;
	private List<MediaType> mediaTypes;
	private boolean newSession;

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

			}
		}
		String accept = getHeader(HttpHeaders.Names.ACCEPT);
		mediaTypes = MediaType.valueOf(accept);
		String cookiesStr = getHeader(HttpHeaders.Names.COOKIE);
		if (cookiesStr != null) {
			Set<Cookie> cookie = CookieDecoder.decode(cookiesStr);
			for (Cookie c : cookie) {
				cookies.put(c.getName(), new HttpCookie(c));
			}
		}
		QueryStringDecoder decoderQuery = new QueryStringDecoder(getUri());
		queryParams.putAll(decoderQuery.getParameters());
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
		return HttpMethod.GET.getName().equalsIgnoreCase(getMethod().getName());
	}

	public boolean isPost() {
		return HttpMethod.POST.getName().equalsIgnoreCase(getMethod().getName());
	}

	public boolean isPut() {
		return HttpMethod.PUT.getName().equalsIgnoreCase(getMethod().getName());
	}

	public boolean isDelete() {
		return HttpMethod.DELETE.getName().equalsIgnoreCase(getMethod().getName());
	}

	public boolean isHead() {
		return HttpMethod.HEAD.getName().equalsIgnoreCase(getMethod().getName());
	}

	public boolean isOptions() {
		return HttpMethod.OPTIONS.getName().equalsIgnoreCase(getMethod().getName());
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

	public void setNewSession(final boolean newSession) {
		this.newSession = newSession;
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
		return getCookie(HttpServer.SID).getValue();
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
}
