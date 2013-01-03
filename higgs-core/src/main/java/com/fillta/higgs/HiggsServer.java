package com.fillta.higgs;

import com.fillta.functional.Function;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public abstract class HiggsServer<T, OM, IM, SM> extends EventProcessor<T, OM, IM, SM> {

	private int port;
	private ServerBootstrap bootstrap = new ServerBootstrap();
	public Channel channel;

	public HiggsServer(int port) {
		this.port = port;
	}

	/**
	 * Set the server's port. Only has any effect if the server is not already bound to a port.
	 * @param port
	 */
	public void setPort(final int port) {
		this.port = port;
	}

	public void bind() {
		bind(new Function() {
			public void apply() {
				//NO-OP
			}
		});
	}

	public void bind(Function function) {
		try {
			bootstrap.group(parentGroup(), childGroup())
					.channel(channelClass())
					.localAddress(port)
					.childHandler(initializer());
			channel = bootstrap.bind().sync().channel();
			if (function != null) {
				function.apply();
			}
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
