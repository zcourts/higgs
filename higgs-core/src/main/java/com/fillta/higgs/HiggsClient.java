package com.fillta.higgs;

import com.fillta.higgs.events.HiggsEvent;
import com.fillta.higgs.events.listeners.ChannelEventListener;
import com.fillta.higgs.util.Function;
import com.fillta.higgs.util.Function1;
import com.fillta.higgs.util.FunctionNOOP;
import com.fillta.higgs.util.Match;
import com.google.common.base.Optional;
import io.netty.channel.Channel;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.io.IOException;
import java.net.ConnectException;
import java.nio.channels.ClosedChannelException;

/**
 * Clients are "builders" for {@link HiggsClientConnection}s.
 * A single client can be used to build several requests. These requests can and should be
 * completely independent but do not necessarily have to be.
 */
public abstract class HiggsClient<T, OM, IM, SM> extends EventProcessor<T, OM, IM, SM> {

	public <H extends HiggsClientConnection<T, OM, IM, SM>> void
	connect(String serviceName, String host, int port, boolean decompress, boolean useSSL,
	        HiggsInitializer initializer, Function1<H> function) {
		final H request =
				(H) newClientRequest(this, serviceName, host, port, decompress, useSSL, initializer);
		try {
			initReconnect(request, new Function1<HiggsClientConnection<T, OM, IM, SM>>() {
				@Override
				public void call(final HiggsClientConnection<T, OM, IM, SM> req) {
					threadPool.submit(new Runnable() {
						@Override
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
			request.bootstrap.group(group())
					.channel(channel())
					.remoteAddress(host, port)
					.handler(initializer);
			// Make a new connection.
			ChannelFuture f = request.bootstrap.connect().sync();
			request.channel = f.channel();
			request.connected = true;
			function.call(request);
		} catch (InterruptedException ie) {
		}
	}

	protected abstract <H extends HiggsClientConnection<T, OM, IM, SM>> H newClientRequest(
			HiggsClient<T, OM, IM, SM> client, String serviceName,
			String host, int port, boolean decompress, boolean useSSL,
			HiggsInitializer initializer
	);

	protected void initReconnect(final HiggsClientConnection<T, OM, IM, SM> connection, final Function1<HiggsClientConnection<T, OM, IM, SM>> function) {
		//make sure we only add once
		if (!connection.addedReconnectListener) {
			connection.addedReconnectListener = true;
			on(HiggsEvent.EXCEPTION_CAUGHT, new ChannelEventListener() {
				private void sleep(int time) {
					try {
						Thread.sleep(time);
					} catch (InterruptedException e) {
					}
				}

				@Override
				public void triggered(ChannelHandlerContext ctx, Optional<Throwable> ex) {
					if (ex.isPresent()) {
						new Match()
								.caseAssignableFrom(ConnectException.class, ex.get(), new Function() {
									public void call() {
										connection.connected = false;
										sleep(connection.reconnectTimeout);
										log.warn(String.format("Failed to connect to %s on %s:%s, attempting to retry", connection.serviceName, connection.host, connection.port));
										connect(connection.serviceName, connection.host, connection.port,
												connection.decompress, connection.useSSL,
												connection.initializer, function);
									}
								})
								.caseAssignableFrom(ClosedChannelException.class, ex.get(), new Function() {
									public void call() {
										connection.connected = false;
										log.warn(String.format("Client connection to %s on %s:%s socket closed", connection.serviceName, connection.host, connection.port));
									}
								})
								.caseAssignableFrom(IOException.class, ex.get(), new Function() {
									public void call() {
										connection.connected = false;
										sleep(connection.reconnectTimeout);
										log.warn(String.format("Connection to %s on %s:%s, was forcibly closed, the server may be unavailable, attempting to reconnect", connection.serviceName, connection.host, connection.port));
										connect(connection.serviceName, connection.host, connection.port,
												connection.decompress, connection.useSSL,
												connection.initializer, function);
									}
								})
								.caseAssignableFrom(ChannelException.class, ex.get(), new FunctionNOOP() {
									//NOOP
								});
					}
				}
			});
		}
	}

	public Class<? extends Channel> channel() {
		return NioSocketChannel.class;
	}

	protected NioEventLoopGroup group() {
		return new NioEventLoopGroup();
	}
}
