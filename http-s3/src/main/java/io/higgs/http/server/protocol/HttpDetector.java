package io.higgs.http.server.protocol;

import io.higgs.core.ProtocolDetector;
import io.higgs.http.server.HttpRequestDecoder;
import io.higgs.http.server.HttpResponseEncoder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.EmptyByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.stream.ChunkedWriteHandler;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class HttpDetector implements ProtocolDetector {
    protected final HttpProtocolConfiguration config;

    public HttpDetector(HttpProtocolConfiguration config) {
        this.config = config;
    }

    @Override
    public boolean detected(ChannelHandlerContext ctx, ByteBuf in) {
        final int magic1 = in.getUnsignedByte(in.readerIndex());
        final int magic2 = in.getUnsignedByte(in.readerIndex() + 1);
        return
                magic1 == 'G' && magic2 == 'E' || // GET
                        magic1 == 'P' && magic2 == 'O' || // POST
                        magic1 == 'P' && magic2 == 'U' || // PUT
                        magic1 == 'H' && magic2 == 'E' || // HEAD
                        magic1 == 'O' && magic2 == 'P' || // OPTIONS
                        magic1 == 'P' && magic2 == 'A' || // PATCH
                        magic1 == 'D' && magic2 == 'E' || // DELETE
                        magic1 == 'T' && magic2 == 'R' || // TRACE
                        magic1 == 'C' && magic2 == 'O';   // CONNECT
    }

    @Override
    public HttpHandler setupPipeline(ChannelPipeline p, ChannelHandlerContext ctx) {
        //HttpHandler is stateful so must do an instance per request/channel
        HttpHandler h = new HttpHandler(config);
        p.addLast("decoder", new HttpRequestDecoder());
        p.addLast("encoder", new HttpResponseEncoder());
        p.addLast("chunkedWriter", new ChunkedWriteHandler());
        //ByteBufToHttpContent must come before compressor and after chunked writer to support
        //compressing chunked files
//        p.addLast("ByteBufToHttpContent", new MessageToMessageEncoder<ByteBuf>() {
//            @Override
//            protected void encode(ChannelHandlerContext ctx, ByteBuf msg, MessageBuf<Object> out) throws Exception {
//                out.add(new DefaultHttpContent(msg.retain()));
//            }
//        });
//        p.addLast("deflater", new HttpContentCompressor());
        p.addLast("handler", h);
        return h;
    }
}
