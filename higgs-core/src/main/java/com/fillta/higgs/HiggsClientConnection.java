package com.fillta.higgs;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class HiggsClientConnection<T, OM, IM, SM> {
	public enum State {
		UNINITIALIZED,
		CONNECTED,
		CONNECTING,
		DISCONNECTED
	}

	protected Logger log = LoggerFactory.getLogger(getClass());
	protected LinkedBlockingQueue<OM> unsentMessages = new LinkedBlockingQueue();

	private final String host;
	private final int port;
	private final String serviceName;
	private Bootstrap bootstrap;
	private boolean autoReconnect = true;
	private boolean addedReconnectListener;
	private State connected = State.UNINITIALIZED;
	private int reconnectTimeout = 10000;
	private Channel channel;
	private HiggsClient<T, OM, IM, SM> client;
	private boolean decompress;
	private boolean useSSL;
	private HiggsInitializer initializer;

	public HiggsClientConnection(HiggsClient<T, OM, IM, SM> client, String serviceName, String host, int port, boolean decompress, boolean useSSL, HiggsInitializer initializer) {
		this.host = host;
		this.port = port;
		this.serviceName = serviceName;
		this.client = client;
		this.useSSL = useSSL;
		this.decompress = decompress;
		this.initializer = initializer;
	}

	public State getState() {
		return connected;
	}

	public void setState(final State connecting) {
		connected = connecting;
	}

	/**
	 * Sends a message immediately on the current thread, without using any queueing strategies
	 *
	 * @param msg
	 */
	public void send(OM msg) {
		if (connected != State.CONNECTED && !autoReconnect) {
			throw new IllegalStateException(String.format("Client is not connected to a server %s and Auto reconnect is disabled." +
					"The message will not be queued as this could lead to out of memory errors due to the unsent message backlog", serviceName));
		} else {
			if (connected != State.CONNECTED) {
				unsentMessages.add(msg);
			} else {
				channel.write(client.serialize(channel, msg));
			}
		}

	}

	public void newBootstrap() {
//		if (bootstrap != null) {
//			bootstrap.shutdown();
//		}
		bootstrap = new Bootstrap();
	}

	public boolean isAutoReconnectEnabled() {
		return autoReconnect;
	}

	public void setAutoReconnect(final boolean autoReconnect) {
		this.autoReconnect = autoReconnect;
	}

	public boolean isAddedReconnectListener() {
		return addedReconnectListener;
	}

	public void setAddedReconnectListener(final boolean addedReconnectListener) {
		this.addedReconnectListener = addedReconnectListener;
	}

	public boolean isConnected() {
		return connected == State.CONNECTED;
	}

	public void setConnected(final State connected) {
		this.connected = connected;
	}

	public int getReconnectTimeout() {
		return reconnectTimeout;
	}

	public void setReconnectTimeout(final int reconnectTimeout) {
		this.reconnectTimeout = reconnectTimeout;
	}

	public Channel getChannel() {
		return channel;
	}

	public void setChannel(final Channel channel) {
		this.channel = channel;
	}

	public HiggsClient<T, OM, IM, SM> getClient() {
		return client;
	}

	public void setClient(final HiggsClient<T, OM, IM, SM> client) {
		this.client = client;
	}

	public boolean isDecompress() {
		return decompress;
	}

	public void setDecompress(final boolean decompress) {
		this.decompress = decompress;
	}

	public boolean isUseSSL() {
		return useSSL;
	}

	public void setUseSSL(final boolean useSSL) {
		this.useSSL = useSSL;
	}

	public HiggsInitializer getInitializer() {
		return initializer;
	}

	public void setInitializer(final HiggsInitializer initializer) {
		this.initializer = initializer;
	}

	public Bootstrap getBootstrap() {
		return bootstrap;
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	public String getServiceName() {
		return serviceName;
	}
}
