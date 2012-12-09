package com.fillta.higgs.http.client;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpTransferEncoding;
import io.netty.handler.codec.http.HttpVersion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class HTTPResponse {
	public String requestID;
	public HttpTransferEncoding transferEncoding;
	public HttpVersion protocolVersion;
	public HttpResponseStatus status;
	public final StringBuilder data = new StringBuilder();
	public final Map<String, ArrayList<String>> headers = new HashMap();

	public String toString() {
		return String.format("%s\n %s\n %s\n %s\n %s\n", status, transferEncoding, protocolVersion, headers, data.toString());
	}
}
