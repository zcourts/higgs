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
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.net.URI;
import java.util.List;

import static io.netty.handler.codec.http.HttpConstants.CR;
import static io.netty.handler.codec.http.HttpConstants.LF;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class HTTPStreamingRequest extends Request<HTTPStreamingRequest> {
    private static final byte[] CRLF = {CR, LF};

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
        if (!request.headers().contains(HttpHeaders.Names.CONNECTION)) {
            request.headers().remove(HttpHeaders.Names.CONNECTION);
        }
        request.headers().set(HttpHeaders.Names.TRANSFER_ENCODING, HttpHeaders.Values.CHUNKED);
        request.headers().set(HttpHeaders.Names.EXPECT, HttpHeaders.Values.CONTINUE);
        FutureResponse res = super.execute(conf);
        channel.config().setOption(ChannelOption.ALLOW_HALF_CLOSURE, false);
        return res;
    }

    public void onReady(final Function1<StreamSender> listener) {
        if (connectFuture == null) {
            throw new IllegalStateException("Not connected");
        }
        connectFuture.addListener(new GenericFutureListener<ChannelFuture>() {
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    SslHandler sslHandler = channel.pipeline().get("ssl") instanceof SslHandler ?
                            (SslHandler) channel.pipeline().get("ssl") : null;
                    if (sslHandler == null && useSSL) {
                        throw new IllegalStateException("SSL request but 'ssl' handler in the " +
                                "pipeline is not an SslHandler instance");
                    }
                    if (sslHandler != null) {
                        sslHandler.handshakeFuture().addListener(new GenericFutureListener<Future<? super Channel>>() {
                            @Override
                            public void operationComplete(Future<? super Channel> future) throws Exception {
                                if (future.isSuccess()) {
                                    connected();
                                } else {
                                    response.markFailed(future.cause());
                                }
                            }
                        });
                    } else {
                        connected();
                    }
                }
            }

            private void connected() {
                StreamSender sender = new StreamSender(channel);
                List<String> names = channel.pipeline().names();
                for (String name : names) {
                    ChannelHandler handler = channel.pipeline().get(name);
                    if (!(handler instanceof SslHandler) && handler != null) {
                        channel.pipeline().remove(name);
                    }
                }
                //add last, after SSL handler, if present
                channel.pipeline().addLast("raw-content-encoder", new MessageToByteEncoder<ByteBuf>() {
                    @Override
                    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception {
                        //since we removed all other handlers, we have to write the HTTP chunk size ourselves
                        out.writeBytes(Integer.toHexString(msg.readableBytes()).getBytes());
                        out.writeBytes(CRLF);
                        out.writeBytes(msg);
                        out.writeBytes(CRLF);
                    }
                });
                listener.apply(sender);
            }
        });
    }

    public static class StreamSender {
        protected final ObjectMapper MAPPER = new ObjectMapper();
        private final Channel channel;

        public StreamSender(Channel channel) {
            if (channel == null) {
                throw new IllegalArgumentException("Channel cannot be null");
            }
            this.channel = channel;
        }

        public ChannelFuture send(Object content) throws JsonProcessingException {
            return send(channel.alloc().ioBuffer().writeBytes(MAPPER.writeValueAsBytes(content)));
        }

        public ChannelFuture send(final String content) {
            return send(Unpooled.wrappedBuffer(content.getBytes()));
        }

        public synchronized ChannelFuture send(final ByteBuf content) {
            return channel.writeAndFlush(content);
        }
    }
}
