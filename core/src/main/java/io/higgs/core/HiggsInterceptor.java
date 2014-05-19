package io.higgs.core;

import io.netty.channel.ChannelHandlerContext;

/**
 * A request interceptor provides a way for an incoming request to be handled by a the same or a
 * completely different {@link MessageHandler}.
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
public interface HiggsInterceptor {
    /**
     * An interceptor should provide a set of patterns where, if matched and the incoming request is of
     * the class type corresponding to a matched pattern the
     * {@link #intercept(io.netty.channel.ChannelHandlerContext, Object)} method is called passing the request
     * object and the matched pattern. Once a pattern is matched no further action is taken to process
     * the request unless the intercept method returns false, indicating it was not able to process the
     * request.
     * One might store a set of patterns to be matched in the form {@code Map<Pattern, Class<?>>}
     *
     * @return true if, using its set of patterns this request can be handled.
     */
    boolean matches(Object msg);

    /**
     * Invoked when one of the patterns provided by this interceptor matches.
     *
     * @param request The incoming request to be intercepted
     * @return true if the interceptor is successfully able to process the request false otherwise.
     * If false is returned the request may be passed to another interceptor or left to the origin
     * {@link MessageHandler}
     */
    boolean intercept(ChannelHandlerContext ctx, Object request);
}
