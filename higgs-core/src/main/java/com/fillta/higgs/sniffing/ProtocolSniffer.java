package com.fillta.higgs.sniffing;

import com.fillta.higgs.EventProcessor;
import com.fillta.higgs.HiggsEventHandlerProxy;
import com.fillta.higgs.ssl.SSLConfigFactory;
import com.fillta.higgs.ssl.SSLContextFactory;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundByteHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.compression.ZlibCodecFactory;
import io.netty.handler.codec.compression.ZlibWrapper;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.ssl.SslHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLEngine;
import java.util.Set;

/**
 * Manipulates the current pipeline dynamically to switch protocols or enable
 * SSL or GZIP.
 */
public class ProtocolSniffer extends ChannelInboundByteHandlerAdapter {
	private final Logger log = LoggerFactory.getLogger(getClass());
	private final EventProcessor events;
	private final boolean detectSsl;
	private final boolean detectGzip;
	private final Set<ProtocolDetector> detectors;

	private ProtocolSniffer(Set<ProtocolDetector> detectors, EventProcessor events,
	                        boolean detectSsl, boolean detectGZip) {
		this.events = events;
		this.detectSsl = detectSsl;
		this.detectGzip = detectGZip;
		this.detectors = detectors;
	}

	public void inboundBufferUpdated(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
		// Will use the first five bytes to detect a protocol.
		if (in.readableBytes() < 5) {
			return;
		}
		if (isSsl(in)) {
			enableSsl(ctx);
		} else {
			final int magic1 = in.getUnsignedByte(in.readerIndex());
			final int magic2 = in.getUnsignedByte(in.readerIndex() + 1);
			if (isGzip(magic1, magic2)) {
				enableGzip(ctx);
			} else if (isHttp(magic1, magic2)) {
				switchToHttp(ctx);
			} else {
				boolean foundProtocol = false;
				for (ProtocolDetector fn : detectors) {
					if (in.readableBytes() < fn.bytesRequired()) {
						return;
					}
					foundProtocol = fn.apply(in);
					if (foundProtocol) {
						ChannelPipeline pipeline = fn.setupPipeline(ctx);
						pipeline.remove(this);
						break;
					}
				}
				if (!foundProtocol) {
					log.error("Unable to detect the protocol of an incoming connection!");
					// Unknown protocol; discard everything and close the connection.
					in.clear();
					ctx.close();
					return;
				}
			}
		}
		// Forward the current read buffer as is to the new handlers.
		ctx.nextInboundByteBuffer().writeBytes(in);
		ctx.fireInboundBufferUpdated();
	}

	private boolean isSsl(ByteBuf buf) {
		if (detectSsl) {
			return SslHandler.isEncrypted(buf);
		}
		return false;
	}

	private boolean isGzip(int magic1, int magic2) {
		if (detectGzip) {
			//see http://www.gzip.org/zlib/rfc-gzip.html#header-trailer for the magic number definitions
			return magic1 == 31 && magic2 == 139;
		}
		return false;
	}

	private static boolean isHttp(int magic1, int magic2) {
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

	private void enableSsl(ChannelHandlerContext ctx) {
		ChannelPipeline p = ctx.pipeline();
		SSLEngine engine = SSLContextFactory.getSSLSocket(SSLConfigFactory.sslConfiguration).createSSLEngine();
		engine.setUseClientMode(false);

		p.addLast("ssl", new SslHandler(engine));
		p.addLast("unificationA", new ProtocolSniffer(detectors, events, false, detectGzip));
		p.remove(this);
	}

	private void enableGzip(ChannelHandlerContext ctx) {
		ChannelPipeline p = ctx.pipeline();
		p.addLast("gzipdeflater", ZlibCodecFactory.newZlibEncoder(ZlibWrapper.GZIP));
		p.addLast("gzipinflater", ZlibCodecFactory.newZlibDecoder(ZlibWrapper.GZIP));
		p.addLast("unificationB", new ProtocolSniffer(detectors, events, detectSsl, false));
		p.remove(this);
	}

	private void switchToHttp(ChannelHandlerContext ctx) {
		ChannelPipeline p = ctx.pipeline();
		p.addLast("decoder", new HttpRequestDecoder());
		p.addLast("encoder", new HttpResponseEncoder());
		p.addLast("deflater", new HttpContentCompressor());
		p.addLast("handler", new HiggsEventHandlerProxy(events));
		p.remove(this);
	}

}
