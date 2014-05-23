package io.higgs.http.server.providers.context;

import io.higgs.http.server.HttpRequest;
import io.higgs.http.server.HttpResponse;
import io.higgs.http.server.protocol.HttpMethod;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public interface ParamInjector {
    Object[] injectParams(HttpMethod method, HttpRequest request, HttpResponse res, ChannelHandlerContext ctx, Object
            [] params);
}
