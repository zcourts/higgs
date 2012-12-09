package com.fillta.higgs;

import com.fillta.higgs.ssl.SSLConfigFactory;
import com.fillta.higgs.ssl.SSLContextFactory;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.compression.ZlibCodecFactory;
import io.netty.handler.codec.compression.ZlibWrapper;
import io.netty.handler.ssl.SslHandler;

import javax.net.ssl.SSLEngine;

public abstract class HiggsInitializer<IM, OM> extends ChannelInitializer<SocketChannel> {

	final protected boolean useDeflater, useInflater, useCodec, useSSL;
	protected ChannelPipeline pipeline;

	public HiggsInitializer(boolean inflate, boolean deflate, boolean usecodec, boolean ssl) {
		useCodec = usecodec;
		useDeflater = deflate;
		useInflater = inflate;
		useSSL = ssl;
	}

	@Override
	public void initChannel(SocketChannel ch) throws Exception {
		pipeline = ch.pipeline();
		//in all cases if SSL is enabled, add it to the pipeline first
		if (useSSL) {
			pipeline.addLast("ssl", ssl());
		}
		if (useCodec) {
			pipeline.addLast("codec", codec());
		}
		//separate inflater and deflater checks because HTTP will usually only add inflator
		//to automatically deflate incoming data
		if (useDeflater) {
			pipeline.addLast("deflater", deflater());
		}
		if (useInflater) {
			pipeline.addLast("inflater", inflater());
		}
		//if not using a codec both encoder and decoder are required
		if (!useCodec) {
			pipeline.addLast("decoder", decoder());
			pipeline.addLast("encoder", encoder());
		}
		pipeline.addLast("handler", handler());
	}

	public abstract ChannelInboundMessageHandlerAdapter handler();

	public abstract ByteToMessageDecoder<IM> decoder();

	public abstract MessageToByteEncoder<OM> encoder();

	public abstract CombinedChannelHandler codec();

	public SslHandler ssl() {
		SSLEngine engine = SSLContextFactory.getSSLSocket(SSLConfigFactory.sslConfiguration).createSSLEngine();
		engine.setUseClientMode(true);
		return new SslHandler(engine);
	}

	public ChannelInboundHandlerAdapter inflater() {
		//for HTTP this would be return  new HttpContentDecompressor()
		return ZlibCodecFactory.newZlibDecoder(ZlibWrapper.GZIP);
	}

	public ChannelOutboundHandlerAdapter deflater() {
		return ZlibCodecFactory.newZlibEncoder(ZlibWrapper.GZIP);
	}
}
