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

	public HiggsInitializer(boolean inflate, boolean deflate, boolean usecodec, boolean ssl) {
		useCodec = usecodec;
		useDeflater = deflate;
		useInflater = inflate;
		useSSL = ssl;
	}

	int count = 0;

	@Override
	public void initChannel(SocketChannel ch) throws Exception {
		ChannelPipeline pipeline = ch.pipeline();
		//in all cases if SSL is enabled, add it to the pipeline first
		if (useSSL) {
			beforeSSL(pipeline);
			pipeline.addLast("ssl", ssl());
		}
		if (useCodec) {
			beforeCodec(pipeline);
			pipeline.addLast("codec", codec());
		}
		//separate inflater and deflater checks because HTTP will usually only add inflator
		//to automatically deflate incoming data
		if (useDeflater) {
			beforeDeflater(pipeline);
			pipeline.addLast("deflater", deflater());
		}
		if (useInflater) {
			beforeInflater(pipeline);
			pipeline.addLast("inflater", inflater());
		}
		//if not using a codec both encoder and decoder are required
		if (!useCodec) {
			beforeDecoder(pipeline);
			pipeline.addLast("decoder", decoder());
			beforeEncoder(pipeline);
			pipeline.addLast("encoder", encoder());
		}
		beforeHandler(pipeline);
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

	//NO-OP methods that can be overriden to add to the pipeline before each of these are added
	public void beforeSSL(ChannelPipeline pipeline) {
	}

	public void beforeCodec(ChannelPipeline pipeline) {
	}

	public void beforeDeflater(ChannelPipeline pipeline) {
	}

	public void beforeInflater(ChannelPipeline pipeline) {
	}

	public void beforeDecoder(ChannelPipeline pipeline) {
	}

	public void beforeEncoder(ChannelPipeline pipeline) {
	}

	public void beforeHandler(ChannelPipeline pipeline) {
	}
}
