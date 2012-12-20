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
	private String requestID;
	private HttpTransferEncoding transferEncoding;
	private HttpVersion protocolVersion;
	private HttpResponseStatus status;
	private final StringBuilder data = new StringBuilder();
	private final Map<String, ArrayList<String>> headers = new HashMap();

	public String getRequestID() {
		return requestID;
	}

	public void setRequestID(final String requestID) {
		this.requestID = requestID;
	}

	public HttpTransferEncoding getTransferEncoding() {
		return transferEncoding;
	}

	public void setTransferEncoding(final HttpTransferEncoding transferEncoding) {
		this.transferEncoding = transferEncoding;
	}

	public HttpVersion getProtocolVersion() {
		return protocolVersion;
	}

	public void setProtocolVersion(final HttpVersion protocolVersion) {
		this.protocolVersion = protocolVersion;
	}

	public HttpResponseStatus getStatus() {
		return status;
	}

	public void setStatus(final HttpResponseStatus status) {
		this.status = status;
	}


	public String toString() {
		return String.format("%s\n %s\n %s\n %s\n %s\n", status, transferEncoding, protocolVersion, headers, data.toString());
	}

	public String getData() {
		return data.toString();
	}

	public Map<String, ArrayList<String>> getHeaders() {
		return headers;
	}

	public void append(final String data) {
		this.data.append(data);
	}
}
