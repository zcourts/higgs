package com.fillta.higgs.sniffing;

import com.fillta.higgs.EventProcessor;
import com.fillta.higgs.HiggsEventHandlerProxy;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.*;
import io.netty.handler.stream.ChunkedWriteHandler;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class HttpDetector extends ProtocolDetector {
	EventProcessor<String, HttpResponse, HttpRequest, Object> events;

	public HttpDetector(EventProcessor<String, HttpResponse, HttpRequest, Object> events) {
		this.events = events;
	}

	public Boolean apply(final ByteBuf in) {
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

	public boolean setupPipeline(final ChannelHandlerContext ctx) {
		ChannelPipeline p = ctx.pipeline();
		p.addLast("decoder", new HttpRequestDecoder());
		p.addLast("encoder", new HttpResponseEncoder());
		p.addLast("deflater", new HttpContentCompressor());
		p.addLast("chunkedWriter", new ChunkedWriteHandler());
		p.addLast("handler", new HiggsEventHandlerProxy(events));
		return true;
	}

	public int bytesRequired() {
		return 2;
	}
}
