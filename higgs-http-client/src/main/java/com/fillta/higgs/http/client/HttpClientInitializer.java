package com.fillta.higgs.http.client;

import com.fillta.higgs.EventProcessor;
import com.fillta.higgs.HiggsCodecInitializer;
import com.fillta.higgs.HiggsEventHandlerProxy;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInboundMessageHandlerAdapter;
import io.netty.channel.CombinedChannelHandler;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.stream.ChunkedWriteHandler;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class HttpClientInitializer<T, OM, IM, SM> extends HiggsCodecInitializer<IM, OM> {
	EventProcessor<T, OM, IM, SM> events;

	public HttpClientInitializer(EventProcessor<T, OM, IM, SM> events, boolean inflate, boolean ssl) {
		super(inflate, false, ssl);
		this.events = events;
	}

	@Override
	public ChannelInboundMessageHandlerAdapter handler() {
		//add chunked writer before handler is added
		pipeline.addLast("chunkedWriter", new ChunkedWriteHandler());
		return new HiggsEventHandlerProxy(events);
	}

	@Override
	public CombinedChannelHandler codec() {
		return new HttpClientCodec();
	}

	@Override
	public ChannelInboundHandlerAdapter inflater() {
		return new HttpContentDecompressor();
	}
}
