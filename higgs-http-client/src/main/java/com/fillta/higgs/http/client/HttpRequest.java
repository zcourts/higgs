package com.fillta.higgs.http.client;

import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class HttpRequest extends DefaultHttpRequest {
	public final HttpRequestBuilder req;
	public final String id;

	/**
	 * Creates a new instance.
	 *
	 * @param httpVersion the HTTP version of the request
	 * @param method      the HTTP method of the request
	 * @param uri         the URI or path of the request
	 */
	public HttpRequest(HttpRequestBuilder req, HttpVersion httpVersion, HttpMethod method, String uri) {
		super(httpVersion, method, uri);
		this.req = req;
		id = req.url() + "-" + System.nanoTime();
	}
}
