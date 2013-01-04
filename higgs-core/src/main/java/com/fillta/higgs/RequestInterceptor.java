package com.fillta.higgs;

import com.fillta.higgs.EventProcessor;
import io.netty.channel.ChannelHandlerContext;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * A request interceptor provides a way for an incoming request to be handled by a the same or a
 * completely different {@link EventProcessor}.
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
public interface RequestInterceptor {
	/**
	 * Provides a set of patterns where, if matched and the incoming request is of the class type
	 * corresponding to the matched pattern the {@link #intercept(ChannelHandlerContext, Object, Pattern)}
	 * method is called passing the request object and the matched pattern.
	 * Once a pattern is matched no further action is taken to process the request unless
	 * the intercept method returns false, indicating it was not able to process the request.
	 *
	 * @return a set of patterns that must match for this interceptor to be used
	 */
	public Map<Pattern, Class<?>> getInterceptionPatterns();

	/**
	 * Invoked when one of the patterns provided by this interceptor matches.
	 *
	 * @param request The incoming request to be intercepted
	 * @param pattern The pattern which matched and resulted in this interception
	 * @return true if the interceptor is successfully able to process the request false otherwise.
	 *         If false is returned the request may be passed to another interceptor or left to the origin
	 *         {@link EventProcessor}
	 */
	public boolean intercept(ChannelHandlerContext ctx, Object request, Pattern pattern);
}
