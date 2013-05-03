package io.higgs.http.server.protocol;

import io.netty.channel.ChannelHandlerContext;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public interface ParamInjector {
    Object[] injectParams(HttpMethod method, HttpRequest request, ChannelHandlerContext ctx);
}
