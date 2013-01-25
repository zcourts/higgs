package com.fillta.higgs;

import com.fillta.functional.Function1;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Returned from clients when they attempt to make a connection
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class ConnectFuture<T, OM, IM, SM> implements Future {
	public static enum State {
		UNINITIALIZED, DISCONNECTED, CONNECTING, CONNECTED
	}

	protected Logger log = LoggerFactory.getLogger(getClass());
	protected boolean cancelled = true;
	protected ChannelFuture future;
	protected Set<Function1<Channel>> listeners =
			Collections.newSetFromMap(new ConcurrentHashMap<Function1<Channel>, Boolean>());
	protected LinkedBlockingQueue<OM> unsentMessages = new LinkedBlockingQueue<>();
	protected HiggsClient<T, OM, IM, SM> client;
	protected boolean reconnectListenerAdded;
	protected State state = State.UNINITIALIZED;

	public ConnectFuture(final HiggsClient<T, OM, IM, SM> client,
	                     boolean reconnect) {
		cancelled = !reconnect;
		this.client = client;
	}

	public void setFuture(ChannelFuture f) {
		if (f != null) {
			future = f;
			addFutureListener();
		}
	}

	private void addFutureListener() {
		future.addListener(new ChannelFutureListener() {
			public void operationComplete(final ChannelFuture future) throws Exception {
				if (future.isSuccess()) {
					doCallbacks(future.channel());
				} else if (cancelled) {
					//only if failed and cancelled should we invoke with null in any other cause
					//a reconnect will be attempted
					doCallbacks(null);
				}
			}

			private void doCallbacks(final Channel c) {
				log.debug(String.format("Connected, number of unsent messages {%s}", unsentMessages.size()));
				if (c != null && unsentMessages.size() > 0) {
					client.threadPool().submit(new Runnable() {
						public void run() {
							OM msg;
							while (c.isActive() && (msg = unsentMessages.poll()) != null) {
								send(msg);
							}
						}
					});
				}
				for (Function1<Channel> fn : listeners) {
					fn.apply(c);
				}
			}
		});
	}

	public boolean cancel(final boolean mayInterruptIfRunning) {
		if (cancelled) {
			return false;
		}
		return cancelled = true;
	}

	public boolean isCancelled() {
		return cancelled;
	}

	public boolean isDone() {
		return future.isDone();
	}

	public ConnectFuture addListener(Function1<Channel> callback) {
		if (callback != null) {
			listeners.add(callback);
		}
		return this;
	}

	public Channel get() {
		return future.channel();
	}

	public Object get(final long timeout, final TimeUnit unit) {
		Channel channel = future.channel();
		if (channel == null) {
			try {
				Thread.sleep(unit.toMillis(timeout));
				channel = future.channel();
			} catch (InterruptedException e) {
			}
		}
		return channel;
	}

	public ConnectFuture<T, OM, IM, SM> send(OM msg) {
		Channel c = future.channel();
		if (isDone() && c != null && c.isActive()) {
			c.write(client.serialize(c, msg));
			c.flush();
		} else {
			if (c == null && cancelled) {
				throw new IllegalStateException("Cannot queue a message when no channel is available " +
						"and reconnect is not enabled");
			}
			unsentMessages.add(msg);
		}
		return this;
	}

	public boolean isReconnectListenerAdded() {
		return reconnectListenerAdded;
	}

	public void setReconnectListenerAdded(final boolean reconnectListenerAdded) {
		this.reconnectListenerAdded = reconnectListenerAdded;
	}

	public State getState() {
		return state;
	}

	public void setState(State state) {
		this.state = state;
	}

	public HiggsClient<T, OM, IM, SM> client() {
		return client;
	}
}
