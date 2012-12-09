package com.fillta.higgs;

import com.fillta.higgs.events.ChannelMessage;
import com.fillta.higgs.util.Function1;

/**
 * A client request where the outgoing and incoming message are the same types
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class HiggsSingleMessageClientRequest<T, M, SM> extends HiggsClientRequest<T, M, M, SM> {

    public HiggsSingleMessageClientRequest(final HiggsClient<T, M, M, SM> client, String serviceName, String host, int port, boolean decompress, boolean useSSL, HiggsInitializer initializer) {
        super(client, serviceName, host, port, decompress, useSSL, initializer);
        client.messageQueue.listen(new Function1<ChannelMessage<M>>() {
            public void call(ChannelMessage<M> a) {
                //only outgoing messages are handled
                if (a.isOutGoing) {
                    channel.write(client.serialize(channel, a.message));
                }
            }
        });
    }

    public void send(M msg) {
        if (!connected && !autoReconnect) {
            throw new IllegalStateException(String.format("Client is not connected to a server %s and Auto reconnect is disabled." +
                    "The message will not be queued as this could lead to out of memory errors due to the unsent message backlog", serviceName));
        } else {
            if (!connected) {
                unsentMessages.add(msg);
            } else {
                client.messageQueue.enqueue(null, msg);
            }
        }

    }
}
