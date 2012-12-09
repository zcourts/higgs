package com.fillta.higgs;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public abstract class HiggsServer<T, OM, IM, SM> extends EventProcessor<T, OM, IM, SM> {

	private final int port;
	private ServerBootstrap bootstrap = new ServerBootstrap();
	public Channel channel;

	public HiggsServer(int port) {
		this.port = port;
	}

	public void bind() {
		try {
			bootstrap.group(parentGroup(), childGroup())
					.channel(channelClass())
					.localAddress(port)
					.childHandler(initializer());
			channel = bootstrap.bind().sync().channel();
		} catch (InterruptedException ie) {
		}
	}

	public abstract ChannelInitializer<SocketChannel> initializer();

	public EventLoopGroup parentGroup() {
		return new NioEventLoopGroup();
	}

	public EventLoopGroup childGroup() {
		return new NioEventLoopGroup();
	}

	public Class<? extends Channel> channelClass() {
		return NioServerSocketChannel.class;
	}
}
