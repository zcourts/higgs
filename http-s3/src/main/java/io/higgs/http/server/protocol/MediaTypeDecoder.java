package io.higgs.http.server.protocol;

import io.higgs.core.reflect.dependency.DependencyProvider;
import io.higgs.http.server.resource.MediaType;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpContent;

import java.util.List;

/**
 * @author Courtney Robinson <courtney.robinson@datasift.com>
 */
public interface MediaTypeDecoder {
    /**
     * @param mediaType the media type to check compatibility for
     * @return true if this decoder can handle the given media type
     */
    boolean canDecode(List<MediaType> mediaType);

    /**
     * Called each time a chunk of data is received for the request
     *
     * @param chunk the chunk to decode or buffer
     */
    void offer(HttpContent chunk);

    /**
     * Invoked when all the HTTP content for the request has een received. {@link #offer(HttpContent)} will not be
     * called after this and whatever is decoded will be passed to the target method
     *
     * @param ctx the context
     */
    void finished(ChannelHandlerContext ctx);

    /**
     * @return A dependency provider which the decoder can use to provide anything that can be injected into methods
     */
    DependencyProvider provider();
}
