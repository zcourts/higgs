package com.fillta.higgs.http.server;

import com.fillta.higgs.http.server.config.ServerConfig;
import com.fillta.higgs.sniffing.ProtocolDetector;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.stream.ChunkedWriteHandler;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class HttpDetector<C extends ServerConfig> implements ProtocolDetector {
	HttpServer<C> events;

	public HttpDetector(HttpServer<C> events) {
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
		ChannelPipeline pipeline = ctx.pipeline();
		pipeline.addLast("decoder", new HttpRequestDecoder());
		pipeline.addLast("encoder", new HttpResponseEncoder());
		//pipeline.addLast("deflater", new HttpContentCompressor());
		pipeline.addLast("chunkedWriter", new ChunkedWriteHandler());
		pipeline.addLast("handler", events);
		return true;
	}

	public int bytesRequired() {
		return 2;
	}
}
