package com.fillta.higgs.ws;

import com.fillta.higgs.http.server.HttpServer;
import com.fillta.higgs.ws.flash.FlashPolicyFile;

/**
 * For documentation see {@link WebSocketServer}.
 * This class's sole purpose is to automatically provide the request type class that
 * {@link WebSocketServer} expects.
 * If you wish to provide your own implementation of the {@link JsonRequestEvent} interface
 * then use {@link WebSocketServer} and be sure to call {@link WebSocketServer#setRequestClass(Class)}
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class DefaultWebSocketServer extends WebSocketServer<JsonRequest> {
	public DefaultWebSocketServer(int port) {
		super(port);
	}

	public DefaultWebSocketServer(HttpServer http, FlashPolicyFile policy, String path, int port) {
		super(http, policy, path, port);
	}
}
