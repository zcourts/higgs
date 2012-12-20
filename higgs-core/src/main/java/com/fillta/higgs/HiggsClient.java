package com.fillta.higgs;

import com.fillta.functional.Function1;
import com.fillta.higgs.events.HiggsEvent;
import com.fillta.higgs.events.listeners.ChannelEventListener;
import com.google.common.base.Optional;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.io.IOException;
import java.net.BindException;
import java.net.ConnectException;
import java.nio.channels.ClosedChannelException;

/**
 * Clients are "builders" for {@link HiggsClientConnection}s.
 * A single client can be used to build several requests. These requests can and should be
 * completely independent but do not necessarily have to be.
 */
public abstract class HiggsClient<T, OM, IM, SM> extends EventProcessor<T, OM, IM, SM> {

	private final NioEventLoopGroup nioEventLoopGroup;

	public HiggsClient() {
		nioEventLoopGroup = new NioEventLoopGroup(maxThreads);
	}

	public <H extends HiggsClientConnection<T, OM, IM, SM>> HiggsClient<T, OM, IM, SM>
	connect(final String serviceName, final String host, final int port, final boolean decompress,
	        final boolean useSSL, final HiggsInitializer initializer, final Function1<H> function) {
		final H request =
				(H) newClientRequest(this, serviceName, host, port, decompress, useSSL, initializer);
		return connect(request, function);
	}

	public <H extends HiggsClientConnection<T, OM, IM, SM>> HiggsClient<T, OM, IM, SM> connect(final H request, final Function1<H> function) {
		if (request.getState() == HiggsClientConnection.State.CONNECTING) {
			return this; //already connecting
		}
		request.setState(HiggsClientConnection.State.CONNECTING);
		threadPool().submit(new Runnable() {
			public void run() {
				if (!request.isAddedReconnectListener() && request.isAutoReconnectEnabled()) {
					addReconnectListener(request, function);
				}
				//use clean bootstrap and clean newInitializer on every connection
				request.newBootstrap();
				request.setInitializer(newInitializer(request.isDecompress(), request.isDecompress(),
						request.isUseSSL()));
				request.getBootstrap()
						.group(group())
						.channel(channelClass())
						.remoteAddress(request.getHost(), request.getPort())
						.handler(request.getInitializer());
				// Make a new connection.
				ChannelFuture f = request.getBootstrap().connect();
				f.addListener(new ChannelFutureListener() {
					public void operationComplete(ChannelFuture future) throws Exception {
						if (future.isSuccess()) {
							log.debug(String.format("Connected to %s", request.getServiceName()));
							request.setChannel(future.channel());
							request.setConnected(HiggsClientConnection.State.CONNECTED);
							function.apply(request);
						} else {
							if (request.isAutoReconnectEnabled()) {
								log.debug(String.format("Connecting to %s failed. Attempting to retry in %s milliseconds", request.getHost(), request.getReconnectTimeout()), future.cause());
								Thread.sleep(request.getReconnectTimeout());
								connect(request, function);
							} else {
								log.debug(String.format("Connecting to %s failed. Auto-reconnect is disabled, your must manually re-connect", request.getHost()), future.cause());
							}
						}
					}
				});
				f.channel().closeFuture().addListener(new ChannelFutureListener() {
					public void operationComplete(final ChannelFuture future) throws Exception {
						//can't shutdown, event loop group is shared and this shuts it down
						//request.getBootstrap().shutdown();
					}
				});
			}
		});
		return this;
	}

	private <H extends HiggsClientConnection<T, OM, IM, SM>> void addReconnectListener(final H request, final Function1<H> function) {
		initReconnect(request, new Function1<HiggsClientConnection<T, OM, IM, SM>>() {
			public void apply(final HiggsClientConnection<T, OM, IM, SM> req) {
				//invoke the on connect callback
				function.apply(request);
				//send any unset/queued messages on a background thread
				threadPool().submit(new Runnable() {
					public void run() {
						//send all messages that were buffered since disconnected on background thread
						while (request.unsentMessages.size() > 0) {
							OM msg = request.unsentMessages.poll();
							if (msg != null) {
								request.send(msg);
							}
						}
					}
				});
			}
		});
	}

	protected abstract <H extends HiggsClientConnection<T, OM, IM, SM>> H newClientRequest(
			HiggsClient<T, OM, IM, SM> client, String serviceName,
			String host, int port, boolean decompress, boolean useSSL,
			HiggsInitializer initializer
	);

	public abstract <H extends HiggsInitializer<IM, OM>> H newInitializer(boolean inflate, boolean deflate, boolean ssl);

	protected void initReconnect(final HiggsClientConnection<T, OM, IM, SM> connection, final Function1<HiggsClientConnection<T, OM, IM, SM>> function) {
		//make sure we only add once
		if (!connection.isAddedReconnectListener()) {
			connection.setAddedReconnectListener(true);
			on(HiggsEvent.EXCEPTION_CAUGHT, new ChannelEventListener() {
				private void sleep(int time) {
					try {
						Thread.sleep(time);
					} catch (InterruptedException e) {
					}
				}

				public void triggered(ChannelHandlerContext ctx, Optional<Throwable> ex) {
					if (ex.isPresent()) {
						if (ex.get() instanceof ConnectException) {
							connection.setConnected(HiggsClientConnection.State.DISCONNECTED);
							log.warn(String.format("Failed to connect to %s on %s:%s, attempting to retry", connection.getServiceName(), connection.getHost(), connection.getPort()));
							sleep(connection.getReconnectTimeout());
							connect(connection, function);
						} else if (ex.get() instanceof ClosedChannelException) {
							connection.setConnected(HiggsClientConnection.State.DISCONNECTED);
							log.warn(String.format("Client connection to %s on %s:%s socket closed", connection.getServiceName(), connection.getHost(), connection.getPort()));
						} else if (ex.get() instanceof IOException) {
							connection.setConnected(HiggsClientConnection.State.DISCONNECTED);
							log.warn(String.format("Connection to %s on %s:%s, was forcibly closed, the server may be unavailable, attempting to reconnect", connection.getServiceName(), connection.getHost(), connection.getPort()));
							sleep(connection.getReconnectTimeout());
							connect(connection, function);
						} else if (ex.get() instanceof ChannelException) {
							if (ex.isPresent() && ex.get() instanceof BindException) {
								log.error(String.format("Cannot start service on localhost:%s address already in use", connection.getPort()), ex.get());
								//start up error. should not continue
							}
						}
					}
				}
			});
		}
	}

	public Class<? extends Channel> channelClass() {
		return NioSocketChannel.class;
	}

	protected EventLoopGroup group() {
		return nioEventLoopGroup;
	}
}
