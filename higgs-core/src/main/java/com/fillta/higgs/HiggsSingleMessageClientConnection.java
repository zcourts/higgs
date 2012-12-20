package com.fillta.higgs;

import com.fillta.functional.Function1;
import com.fillta.higgs.events.ChannelMessage;

/**
 * A client request where the outgoing and incoming message are the same types
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class HiggsSingleMessageClientConnection<T, M, SM> extends HiggsClientConnection<T, M, M, SM> {

	public HiggsSingleMessageClientConnection(final HiggsClient<T, M, M, SM> client, String serviceName, String host, int port, boolean decompress, boolean useSSL, HiggsInitializer initializer) {
		super(client, serviceName, host, port, decompress, useSSL, initializer);
		client.messageQueue.listen(new Function1<ChannelMessage<M>>() {
			public void apply(ChannelMessage<M> a) {
				//only outgoing messages are handled
				if (a.isOutGoing) {
					getChannel().write(client.serialize(getChannel(), a.message));
					getChannel().flush();
				}
			}
		});
	}

	public void send(M msg) {
		if (!isConnected() && !isAutoReconnectEnabled()) {
			throw new IllegalStateException(String.format("Client is not connected to a server %s and Auto reconnect is disabled." +
					"The message will not be queued as this could lead to out of memory errors due to the unsent message backlog", getServiceName()));
		} else {
			if (!isConnected()) {
				unsentMessages.add(msg);
			} else {
				getClient().messageQueue.enqueue(null, msg);
			}
		}

	}
}
