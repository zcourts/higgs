package com.fillta.higgs;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class HiggsClientRequest<T, OM, IM, SM> {
    protected Logger log = LoggerFactory.getLogger(getClass());
    public final String host;
    public final int port;
    public final String serviceName;
    public final Bootstrap bootstrap = new Bootstrap();

    protected LinkedBlockingQueue<OM> unsentMessages = new LinkedBlockingQueue();
    public boolean autoReconnect = true;
    public boolean addedReconnectListener;
    protected boolean connected;
    public int reconnectTimeout = 10000;
    public Channel channel;
    public HiggsClient<T, OM, IM, SM> client;
    public boolean decompress;
    public boolean useSSL;
    public HiggsInitializer initializer;

    public HiggsClientRequest(HiggsClient<T, OM, IM, SM> client, String serviceName, String host, int port, boolean decompress, boolean useSSL, HiggsInitializer initializer) {
        this.host = host;
        this.port = port;
        this.serviceName = serviceName;
        this.client = client;
        this.useSSL = useSSL;
        this.decompress = decompress;
        this.initializer = initializer;
    }

    /**
     * Sends a message immediately on the current thread, without using any queueing strategies
     *
     * @param msg
     */
    public void send(OM msg) {
        if (!connected && !autoReconnect) {
            throw new IllegalStateException(String.format("Client is not connected to a server %s and Auto reconnect is disabled." +
                    "The message will not be queued as this could lead to out of memory errors due to the unsent message backlog", serviceName));
        } else {
            if (!connected) {
                unsentMessages.add(msg);
            } else {
                channel.write(client.serialize(channel, msg));
            }
        }

    }
}
