package io.higgs.http.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.higgs.core.func.Function1;
import io.higgs.http.client.readers.Reader;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpChunkedInput;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.stream.ChunkedInput;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.concurrent.GenericFutureListener;

import java.net.URI;
import java.util.LinkedList;
import java.util.Queue;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class HTTPStreamingRequest extends Request<HTTPStreamingRequest> {
    protected final ObjectMapper MAPPER = new ObjectMapper();
    protected boolean stopped;
    protected ChunkedWriteHandler chunkedHandler;
    protected Queue<ByteBuf> queue = new LinkedList<>();
    protected boolean needToResume;
    protected ChunkedInput<ByteBuf> input = new ChunkedInput<ByteBuf>() {

        @Override
        public boolean isEndOfInput() throws Exception {
            return stopped;
        }

        @Override
        public void close() throws Exception {
        }

        @Override
        public ByteBuf readChunk(ChannelHandlerContext ctx) throws Exception {
            if (stopped || queue.size() == 0) {
                needToResume = true;
                return null;
            } else {
                needToResume = false;
                return queue.poll();
            }
        }
    };

    public HTTPStreamingRequest(HttpRequestBuilder builder, EventLoopGroup group, URI uri, Reader f) {
        super(builder, group, uri, HttpMethod.POST, HttpVersion.HTTP_1_1, f);
    }

    @Override
    protected void newNettyRequest(URI uri, HttpMethod method, HttpVersion version) {
        request = new DefaultHttpRequest(version, method, uri.getRawPath());
        headers().set(HttpHeaders.Names.REFERER, originalUri == null ? uri.toString() : originalUri.toString());
    }

    public FutureResponse execute(Function1<Bootstrap> conf) {
        if (!request.headers().contains(HttpHeaders.Names.CONTENT_TYPE)) {
            request.headers().set(HttpHeaders.Names.CONTENT_TYPE, "application/json");
        }
        if (!request.headers().contains(HttpHeaders.Names.CONTENT_LENGTH)) {
            request.headers().remove(HttpHeaders.Names.CONTENT_LENGTH);
        }
        request.headers().set(HttpHeaders.Names.TRANSFER_ENCODING, HttpHeaders.Values.CHUNKED);
        FutureResponse res = super.execute(conf);
        connectFuture.addListener(new GenericFutureListener<ChannelFuture>() {
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    channel.pipeline().writeAndFlush(new HttpChunkedInput(input));
                    for (String name : channel.pipeline().names()) {
                        ChannelHandler handler = channel.pipeline().get(name);
                        if (handler instanceof ChunkedWriteHandler) {
                            chunkedHandler = ((ChunkedWriteHandler) handler);
                            break;
                        }
                    }
                }
            }
        });
        return res;
    }

    public synchronized HTTPStreamingRequest send(Object content) throws JsonProcessingException {
        return send(Unpooled.wrappedBuffer(MAPPER.writeValueAsBytes(content)));
    }


    public synchronized HTTPStreamingRequest send(final String content) {
//        if (stream.getCtx() == null) {
//            throw new IllegalStateException("Not connected yet");
//        }


//        stream.getCtx().pipeline().writeAndFlush(Unpooled.wrappedBuffer(content.getBytes()));
        return send(Unpooled.wrappedBuffer(content.getBytes()));
    }

    public synchronized HTTPStreamingRequest send(final ByteBuf content) {
        if (chunkedHandler == null) {
            throw new IllegalStateException("ChunkedWriteHandler must be present in the pipeline");
        }
        queue.add(content);
//        ChunkedInput<ByteBuf> input = new ChunkedInput<ByteBuf>() {
//
//            @Override
//            public boolean isEndOfInput() throws Exception {
//                return stopped;
//            }
//
//            @Override
//            public void close() throws Exception {
//            }
//
//            @Override
//            public ByteBuf readChunk(ChannelHandlerContext ctx) throws Exception {
//                if (stopped) {
//                    needToResume = true;
//                    return null;
//                } else {
//                    needToResume = false;
//                    return content;
//                }
//            }
//        };
        channel.pipeline().writeAndFlush(new HttpChunkedInput(input));
        if (needToResume) {
            chunkedHandler.resumeTransfer();
        }
        return this;
    }
//    public static class HttpStreamEncoder extends HttpRequestEncoder {
//        private ChannelHandlerContext ctx;
//
//        public ChannelHandlerContext getCtx() {
//            return ctx;
//        }
//
//        @Override
//        public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
//            super.handlerAdded(ctx);
//            this.ctx = ctx;
//        }
//    }
}
