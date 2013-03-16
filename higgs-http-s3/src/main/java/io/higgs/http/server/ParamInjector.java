package io.higgs.http.server;

import io.higgs.events.ChannelMessage;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public interface ParamInjector {
    /**
     * @param server
     * @param params
     * @param message
     * @param args
     */
    void injectParams(HttpServer server, Endpoint.MethodParam[] params,
                      ChannelMessage<HttpRequest> message, Object[] args);
}
