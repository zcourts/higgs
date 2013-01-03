package com.fillta.higgs.http.server;

/**
 * A resource filter is responsible for taking a request and extracting a registered end point
 * that should be invoked for the request
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
public interface ResourceFilter {

	/**
	 * check this request to see if it matches any registered endpoint
	 *
	 * @param request
	 * @return an end point or null if no endpoint is found
	 */
	public Endpoint getEndpoint(HttpRequest request);
}
