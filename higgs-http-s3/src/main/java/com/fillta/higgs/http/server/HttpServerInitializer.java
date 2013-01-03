package com.fillta.higgs.http.server;

import com.fillta.higgs.HiggsEncoderDecoderInitializer;
import com.fillta.higgs.HiggsEventHandlerProxy;
import io.netty.channel.ChannelInboundMessageHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.stream.ChunkedWriteHandler;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class HttpServerInitializer extends HiggsEncoderDecoderInitializer<Object, Object> {
	HttpServer events;
	private boolean autoCompression;

	public HttpServerInitializer(HttpServer events, boolean autoCompression, boolean ssl) {
		//false,false=not using codec and set SSL client mode to false
		super(false, false, ssl, false, false);
		this.events = events;
		this.autoCompression = autoCompression;
	}

	@Override
	public ChannelInboundMessageHandlerAdapter handler() {
		return new HiggsEventHandlerProxy(events);
	}

	public void beforeHandler(ChannelPipeline pipeline) {
		//add chunked writer before handler is added
		pipeline.addLast("chunkedWriter", new ChunkedWriteHandler());
		//automatic compression
		if (autoCompression) {
			pipeline.addLast("deflater", new HttpContentCompressor());
		}
	}

	public void beforeEncoder(ChannelPipeline pipeline) {
		//if added, causes exceptions with post/put requests larger than configured size
		//add before encoder is added
		//pipeline.addLast("aggregator", new HttpChunkAggregator(65536));
	}

	@Override
	public ByteToMessageDecoder<Object> decoder() {
		return new HttpRequestDecoder();
	}

	@Override
	public MessageToByteEncoder<Object> encoder() {
		return new HttpResponseEncoder();
	}
}
