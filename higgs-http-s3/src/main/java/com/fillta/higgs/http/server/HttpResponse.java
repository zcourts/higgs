package com.fillta.higgs.http.server;

import com.fillta.higgs.http.server.params.HttpCookie;
import io.netty.handler.codec.http.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class HttpResponse extends DefaultHttpResponse {
	private Map<String, HttpCookie> cookies = new HashMap<>();

	/**
	 * Creates a new instance.
	 *
	 * @param version the HTTP version of this response
	 * @param status  the status of this response
	 */
	public HttpResponse(HttpVersion version, HttpResponseStatus status) {
		super(version, status);
	}

	public HttpResponse(HttpResponseStatus status) {
		super(HttpVersion.HTTP_1_1, status);
	}

	/**
	 * Initializes a response with 200 status and sets the connection header to whatever the client
	 * requested. If no connection header is found in the client request then it is set to CLOSE
	 *
	 * @param message
	 */
	public HttpResponse(final HttpRequest message) {
		this(message.getProtocolVersion(), HttpStatus.OK);
		String conn = message.getHeader(HttpHeaders.Names.CONNECTION);
		if (conn == null) {
			conn = HttpHeaders.Values.CLOSE;
		}
		setHeader(HttpHeaders.Names.CONNECTION, conn);
	}

	/**
	 * creates a 200 ok response
	 */
	public HttpResponse() {
		this(HttpResponseStatus.OK);
	}

	public void setCookies(final Map<String, HttpCookie> cookies) {
		this.cookies.putAll(cookies);
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
		cookies.put(name, cookie);
	}

	public void setCookie(final HttpCookie cookie) {
		cookies.put(cookie.getName(), cookie);
	}

	public void clearHeaders() {
		cookies.clear();
		super.clearHeaders();
	}
	//todo
//	public void setHeader(String name, Object value) {
//		if (HttpHeaders.Names.COOKIE.equalsIgnoreCase(name)) {
//
//		} else {
//			super.setHeader(name, value);
//		}
//	}

	/**
	 * sets any overridden headers
	 */
	protected void finalizeCustomHeaders() {
		setHeader(HttpHeaders.Names.SET_COOKIE,
				ServerCookieEncoder.encode(new ArrayList<Cookie>(cookies.values())));

	}
}
