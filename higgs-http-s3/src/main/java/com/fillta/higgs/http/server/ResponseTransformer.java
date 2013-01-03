package com.fillta.higgs.http.server;

import java.util.Queue;

/**
 * The server uses a last in first out queue of transformers. i.e. The last transformer registered with the server
 * will be the first transformer used to process responses. That transformer can decide to try using another
 * transformer but the server will always use the first one returned by the queue (which is the last one added...).
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
public interface ResponseTransformer {
	/**
	 * Determines if, given the response object and the media types accepted by the client this
	 * transformer can convert the response object into one of the accepted types
	 *
	 * @param response the response object that would need to be transformed
	 * @param request  the request which generated the response
	 * @return true if this transformer can convert the response to one of the supported media types...
	 */
	public boolean canTransform(final Object response, final HttpRequest request);

	/**
	 * Given the response object transform it into one of the accepted media types
	 *
	 * @param response               the response object to be transformed
	 * @param request                the request which generated the response
	 * @param registeredTransformers a queue of all transformers registered with the server.
	 *                               If for whatever reason this transformer cannot transform a response
	 *                               it should attempt to use one of the other available transformers.
	 *                               NOTE: this queue of transformers will include the current transformer
	 *                               so implementations must check they are not calling themselves!
	 *                               + Don't remove anything from the queue...the server won't re-add them.  @return an HTTP response. If null is returned the server will return 406 Not Acceptable to the client...
	 *                               (i.e. The requested resource is only capable of generating content not acceptable according to the
	 *                               Accept headers sent in the request.)
	 */
	public HttpResponse transform(final HttpServer server, final Object response, HttpRequest request,
	                              final Queue<ResponseTransformer> registeredTransformers);
}
