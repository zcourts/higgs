package io.higgs.ws.protocol;

import io.higgs.http.server.protocol.HttpDetector;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.stream.ChunkedWriteHandler;

/**
 * An HTTP detector which only handles GET requests
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class WebSocketDetector extends HttpDetector {
    protected final WebSocketConfiguration config;

    public WebSocketDetector(WebSocketConfiguration config) {
        super(config);
        this.config = config;
    }

    @Override
    public boolean detected(ChannelHandlerContext ctx, ByteBuf in) {
        final int magic1 = in.getUnsignedByte(in.readerIndex());
        final int magic2 = in.getUnsignedByte(in.readerIndex() + 1);
        return magic1 == 'G' && magic2 == 'E'; //GET request
    }

    @Override
    public WebSocketHandler setupPipeline(ChannelPipeline p, ChannelHandlerContext ctx) {
        //WebSocketHandler is stateful so must do an instance per request/channel
        WebSocketHandler h = new WebSocketHandler(config);
        p.addLast("ws-decoder", new HttpRequestDecoder());
        p.addLast("ws-aggregator", new HttpObjectAggregator(65536));
        p.addLast("ws-encoder", new HttpResponseEncoder());
        p.addLast("ws-chunkedWriter", new ChunkedWriteHandler());
        p.addLast("ws-handler", h);
        return h;
    }
}
