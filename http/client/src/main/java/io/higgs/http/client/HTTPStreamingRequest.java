package io.higgs.http.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.higgs.core.func.Function1;
import io.higgs.http.client.readers.Reader;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpVersion;

import java.net.URI;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class HTTPStreamingRequest extends Request<HTTPStreamingRequest> {
    protected final ObjectMapper MAPPER = new ObjectMapper();
    protected HttpStreamEncoder stream = new HttpStreamEncoder();

    public HTTPStreamingRequest(HttpRequestBuilder builder, EventLoopGroup group, URI uri, HttpVersion version, Reader f,
                                HttpMethod method) {
        super(builder, group, uri, method, version, f);
    }

    @Override
    protected ChannelHandler newInitializer() {
        return super.newInitializer();
    }

    @Override
    protected ChannelHandler newHandler() {
        return stream;
    }

    public FutureResponse execute(Function1<Bootstrap> conf) {
        if (!request.headers().contains(HttpHeaders.Names.CONTENT_TYPE)) {
            request.headers().set(HttpHeaders.Names.CONTENT_TYPE, "application/json");
        }
        if (!request.headers().contains(HttpHeaders.Names.CONTENT_LENGTH)) {
            request.headers().remove(HttpHeaders.Names.CONTENT_LENGTH);
        }
        request.headers().set(HttpHeaders.Names.TRANSFER_ENCODING, HttpHeaders.Values.CHUNKED);
        return super.execute(conf);
    }

    public HTTPStreamingRequest send(Object content) {

        return this;
    }

    public HTTPStreamingRequest send(String content) {
        if (stream.getCtx() == null) {
            throw new IllegalStateException("Not connected yet");
        }
        stream.getCtx().writeAndFlush(Unpooled.wrappedBuffer(content.getBytes()));
        return this;
    }

    public static class HttpStreamEncoder extends HttpRequestEncoder {
        private ChannelHandlerContext ctx;

        public ChannelHandlerContext getCtx() {
            return ctx;
        }

        @Override
        public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
            super.handlerAdded(ctx);
            this.ctx = ctx;
        }
    }
}
