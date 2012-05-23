package info.crlog.higgs.agents;

import info.crlog.higgs.HiggsEventListener;
import info.crlog.higgs.HiggsServer;
import info.crlog.higgs.messaging.HiggsDecoder;
import info.crlog.higgs.messaging.HiggsEncoder;
import info.crlog.higgs.messaging.Message;
import info.crlog.higgs.messaging.MessageFactory;
import info.crlog.higgs.protocol.boson.BosonMessage;
import io.netty.channel.*;
import java.util.HashMap;
import java.util.Iterator;

/**
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class Broadcaster extends HiggsServer implements HiggsEventListener {

    protected MessageFactory<BosonMessage> factory;
    protected HashMap<Integer, String> subscriptions;

    /**
     * A broadcaster is an agent which binds to the given host and port. Once
     * bound, it accepts "radio" connections. Each connection can "listen" to
     * specified "broadcasts"/"topics". If the radio agent does not specify a
     * topic then all messages that are broadcasted are delivered to that agent.
     *
     * There's no guarantee anyone's listening. If no one's listening the the
     * message is lost.
     *
     * @param host
     * @param port
     */
    public Broadcaster(String host, int port) {
        super(host, port);
    }

    protected void initialize() {
        handler.addEventListener(this);
        factory = new MessageFactory<BosonMessage>(BosonMessage.class);
        decoder = new HiggsDecoder(factory);
        encoder = new HiggsEncoder(factory);
        subscriptions = new HashMap<Integer, String>();
    }

    /**
     * Broadcast a message to all Radio's currently listening Only radio's
     * listening to the given topic will receive this message
     *
     * @param topic The to which the listeners must be subscribed to receive
     * this message.
     * @param content the content of the message
     */
    public void broadcast(String topic, String content) {
        broadcast(factory.newMessage().setTopic(topic).
                setContent(content));
    }

    /**
     * Broadcast a messge with no topic to anyone currently listening
     *
     * @param msg
     */
    public void broadcast(String msg) {
        broadcast(factory.newMessage().setContent(msg));
    }

    /**
     * Send a message to all connected clients
     *
     * @param msg <M extends Message> The message to broadcast, any object whose
     * class implements the {@link Message} interface
     */
    public void broadcast(BosonMessage msg) {
        bind();
        for (Iterator<Channel> it = channels.iterator(); it.hasNext();) {
            Channel c = it.next();
            String topic = subscriptions.get(c.getId());
            //if not subscribed to a topic then broadcast
            boolean dosend = !subscriptions.containsKey(c.getId());
            //if subscribed to a topic
            if (!dosend) {
                //is the subscribed topic and this message's topic the same?
                dosend = msg.getTopic().equals(topic);
            }
            if (c.isWritable() && dosend) {
                c.write(msg);
            }
        }
    }

    /**
     * When a broadcaster gets a message from a Radio, it uses the message's
     * topic as the topic/channel the Radio wants to listen to. Topic
     * subscription can change at any point while the client is connected
     *
     * @param ctx
     * @param e
     */
    public void onMessage(ChannelHandlerContext ctx, MessageEvent e) {
        subscriptions.put(ctx.getChannel().getId(), ((BosonMessage) e.getMessage()).getTopic());
    }

    public void onException(ChannelHandlerContext ctx, ExceptionEvent e) {
        channels.remove(ctx.getChannel());
    }

    public void onDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) {
        channels.remove(ctx.getChannel());
    }

    public void onConnected(ChannelHandlerContext ctx, ChannelStateEvent e) {
        //subscribe to all messages by default
    }

    public void onHandleUpstream(ChannelHandlerContext ctx, ChannelEvent e) {
    }

    /**
     * Prepare for broadcasting. This effectively binds to a local port and
     * allows "Radios" to connect
     */
    public void prepare() {
        bind();
    }

    /**
     * The broadcaster uses a "queue" of sorts when creating new messages. This
     * queue is like a "factory"
     *
     * @param i
     */
    public void setFactorySize(int i) {
        factory.setMaxQueueSize(i);
    }
}
