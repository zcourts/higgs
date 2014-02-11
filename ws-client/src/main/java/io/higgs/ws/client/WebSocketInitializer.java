package io.higgs.ws.client;

import io.higgs.http.client.ClientIntializer;
import io.higgs.http.client.ConnectHandler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponseDecoder;

/**
 * @author Courtney Robinson <courtney.robinson@datasift.com>
 */
public class WebSocketInitializer extends ClientIntializer {
    protected final int maxContentLength;
    private final String fullUrl;

    public WebSocketInitializer(int maxContentLength, boolean ssl, SimpleChannelInboundHandler<Object> handler,
                                ConnectHandler connHandler, String fullUrl) {
        super(ssl, handler, connHandler, sslProtocols);
        this.maxContentLength = maxContentLength;
        this.fullUrl = fullUrl;
    }

    public void configurePipeline(final ChannelPipeline pipeline) {
        if (connectHandler != null) {
            //proxy request
            super.configurePipeline(pipeline);
        } else {
            if (pipeline.get("codec") != null) {
                pipeline.remove("codec");
            }
            if (pipeline.get("inflater") != null) {
                pipeline.remove("inflater");
            }
            if (pipeline.get("chunkedWriter") != null) {
                pipeline.remove("chunkedWriter");
            }
            if (pipeline.get("handler") != null) {
                pipeline.remove("handler");
            }
            //websocket pipeline
            if (fullUrl != null) {
                pipeline.addLast("http-decoder", new HttpResponseDecoder());
                pipeline.addLast("http-encoder", new InterceptingEncoder() {
                    protected void encodeInitialLine(ByteBuf buf, HttpRequest request) throws Exception {
                        //because proxies are going to return a 400 for an "invalid" protocol/scheme
                        //String uri = fullUrl.replace("ws://", "http://");
                        request.setUri(fullUrl);
                        super.encodeInitialLine(buf, request);
                        pipeline.remove(this);
                        pipeline.addLast("http-encoder", new HttpRequestEncoder());
                    }
                });
            } else {
                pipeline.addLast("http-codec", new HttpClientCodec());
            }
            pipeline.addLast("aggregator", new HttpObjectAggregator(maxContentLength));
            pipeline.addLast("ws-handler", handler);
        }
    }

    private static class InterceptingEncoder extends HttpRequestEncoder {
    }
}
