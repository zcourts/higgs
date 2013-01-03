package com.fillta.higgs.http.server;

import com.fillta.higgs.events.ChannelMessage;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public interface ParamInjector {
	/**
	 *
	 * @param server
	 * @param params
	 * @param message
	 * @param args
	 */
	public void injectParams(final HttpServer server, final Endpoint.MethodParam[] params, final ChannelMessage<HttpRequest> message,
	                         final Object[] args);
}
