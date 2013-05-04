package io.higgs.http.server.transformers;

import io.higgs.core.Sortable;
import io.higgs.http.server.protocol.HttpMethod;
import io.higgs.http.server.HttpRequest;
import io.higgs.http.server.HttpResponse;
import io.higgs.http.server.resource.MediaType;
import io.netty.channel.ChannelHandlerContext;

/**
 * The server uses a {@link java.util.TreeSet} of transformers. The first transformer returned is of the highest
 * priority according to the sorted set. That transformer can decide to try using another
 * transformer but the server will always use the first one returned by the queue (which is the last one added...).
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
public interface ResponseTransformer extends Sortable<ResponseTransformer> {
    /**
     * Determines if, given the response object and the media types accepted by the client this
     * transformer can convert the response object into one of the accepted types
     *
     * @param response  the response object that would need to be transformed
     * @param request   the request which generated the response
     * @param mediaType The media type which matched in the {@link HttpMethod}'s produces AND in the client
     *                  set of accepted media types.
     * @return true if this transformer can convert the response to one of the supported media types...
     */
    boolean canTransform(Object response, HttpRequest request, MediaType mediaType,
                         HttpMethod method, ChannelHandlerContext ctx);

    /**
     * Given the response object transform it into one of the accepted media types
     *
     * @param response the response object to be transformed
     * @param request  the request which generated the response
     * @param method   The method which was invoked to produce the response
     * @param ctx      the channel context, provided in the case where an unrecoverable error is
     *                 encountered an error response can be returned by by passing the normal response
     *                 route
     * @return an HTTP response. If null is returned the server will return
     *         406 Not Acceptable to the client...
     *         (i.e. The requested resource is only capable of generating content not acceptable
     *         according to the Accept headers sent in the request.)
     */
    HttpResponse transform(Object response, HttpRequest request, MediaType mediaType, HttpMethod method,
                           ChannelHandlerContext ctx);

    /**
     * @return If a transformer maintains state then this method should return a new instance every time.
     *         If not then this should be returned.
     */
    ResponseTransformer instance();
}
