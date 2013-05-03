package io.higgs.core;

import io.netty.channel.ChannelHandlerContext;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public interface ParameterExtractor {
    Object[] extractParams(ChannelHandlerContext ctx, String path, Object msg);
}
