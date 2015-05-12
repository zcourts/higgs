package io.higgs.http.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.higgs.core.func.Function1;
import io.higgs.http.client.readers.Reader;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
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
        return super.execute(conf);
    }

    public void onReady(final Function1<StreamSender> listener) {
        if (connectFuture == null) {
            throw new IllegalStateException("Not connected");
        }
        connectFuture.addListener(new GenericFutureListener<ChannelFuture>() {
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    StreamSender sender = new StreamSender(channel);
                    for (String name : channel.pipeline().names()) {
                        ChannelHandler handler = channel.pipeline().get(name);
                        if (handler instanceof ChunkedWriteHandler) {
                            sender.setWriteHandler((ChunkedWriteHandler) handler);
                            break;
                        }
                    }
                    if (!sender.hasChunkedHandler()) {
                        throw new IllegalStateException("A chunked write handler must be in the pipeline");
                    }
                    listener.apply(sender);
                }
            }
        });
    }

    public static class StreamSender {
        protected final ObjectMapper MAPPER = new ObjectMapper();
        private final Channel channel;
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

        public StreamSender(Channel channel) {
            if (channel == null) {
                throw new IllegalArgumentException("Channel cannot be null");
            }
            this.channel = channel;
        }

        public StreamSender send(Object content) throws JsonProcessingException {
            return send(Unpooled.wrappedBuffer(MAPPER.writeValueAsBytes(content)));
        }


        public StreamSender send(final String content) {
            return send(Unpooled.wrappedBuffer(content.getBytes()));
        }

        public synchronized StreamSender send(final ByteBuf content) {
            if (chunkedHandler == null) {
                throw new IllegalStateException("ChunkedWriteHandler must be present in the pipeline");
            }
            queue.add(content);
            channel.pipeline().writeAndFlush(new HttpChunkedInput(input));
            if (needToResume) {
                chunkedHandler.resumeTransfer();
            }
            return this;
        }

        public void setWriteHandler(ChunkedWriteHandler writeHandler) {
            this.chunkedHandler = writeHandler;
        }

        public boolean hasChunkedHandler() {
            return chunkedHandler != null;
        }
    }
}
