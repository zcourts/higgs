package info.crlog.higgs.agents;

import info.crlog.higgs.HiggsClient;
import info.crlog.higgs.HiggsEventListener;
import info.crlog.higgs.messaging.HiggsDecoder;
import info.crlog.higgs.messaging.HiggsEncoder;
import info.crlog.higgs.messaging.MessageFactory;
import info.crlog.higgs.messaging.MessageListener;
import info.crlog.higgs.protocol.boson.BosonMessage;
import io.netty.channel.*;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class Radio extends HiggsClient implements HiggsEventListener {

    protected MessageFactory<BosonMessage> factory;
    protected Set<MessageListener> messageListeners;

    public Radio(String host, int port) {
        super(host, port);
    }

    @Override
    protected void initialize() {
        handler.addEventListener(this);
        factory = new MessageFactory<BosonMessage>(BosonMessage.class);
        decoder = new HiggsDecoder(factory);
        encoder = new HiggsEncoder(factory);
        messageListeners = new HashSet<MessageListener>();
    }

    protected void send(String topic, String content) {
        send(new BosonMessage(topic, content));
    }

    protected void send(String topic) {
        send(new BosonMessage(topic, ""));
    }

    protected void send(BosonMessage msg) {
        connect();
        this.channel.write(msg);
    }

    public void onMessage(ChannelHandlerContext ctx, MessageEvent e) {
        for (MessageListener l : messageListeners) {
            l.onMessage((BosonMessage) e.getMessage());
        }
    }

    public void onException(ChannelHandlerContext ctx, ExceptionEvent e) {
        //e.getCause().printStackTrace();
    }

    public void onDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) {
        shutdown();
    }

    public void onConnected(ChannelHandlerContext ctx, ChannelStateEvent e) {
    }

    public void onHandleUpstream(ChannelHandlerContext ctx, ChannelEvent e) {
    }

    /**
     * Start listening to a broadcaster Using this method, all messages sent by
     * the broadcaster will be received
     *
     * @return true if we were able to successfully start listening
     */
    public boolean tune() {
        return tune(null);
    }

    /**
     * Tune/Subscribe to a channel/topic from a broadcaster
     *
     * @param topic Only messages matching this topic will be sent from the
     * broadcaster to you. This acts as a filter so the broadcaster could be
     * publishing other messages that do not match this topic
     * @return true if we were able to successfully start listening
     */
    public boolean tune(String topic) {
        boolean connected = connect();
        if (connected && topic != null) {
            send(topic);
        }
        return connected;
    }

    /**
     * Like broadcasters, a radio uses a message factory. This method allow you
     * to set the maximum empty messages that factory is allowed to keep in its
     * queue
     *
     * @param i
     */
    public void setFactorySize(int i) {
        factory.setMaxQueueSize(i);
    }

    /**
     * Adds a client that will be given all messages received from a broadcaster
     *
     * @param l
     */
    public void addMessageListener(MessageListener l) {
        messageListeners.add(l);
    }

    /**
     * Remove a previously added message listening. Once removed, any further
     * messages received by this radio will not be sent to the removed client
     *
     * @param l
     */
    public void removeMessageListener(MessageListener l) {
        messageListeners.remove(l);
    }
}
