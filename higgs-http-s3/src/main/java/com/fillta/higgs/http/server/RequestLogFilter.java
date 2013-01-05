package com.fillta.higgs.http.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Log HTTP requests
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class RequestLogFilter implements ResourceFilter {
	private Logger log = LoggerFactory.getLogger(getClass());

	public Endpoint getEndpoint(final HttpRequest request) {
		log.info(String.format("%s %s %s", request.getMethod().getName(), request.getUri(), request.getProtocolVersion()));
		//we're only here to log so return null
		return null;
	}
}
