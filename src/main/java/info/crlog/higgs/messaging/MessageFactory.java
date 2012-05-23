package info.crlog.higgs.messaging;

import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The MessageFactory is a "queue" of "empty" messages. It has a ceiling and
 * floor threshold which dictate the minimum and maximum amount of messages to
 * keep in the queue. When the {@link newMessage()} method is invoked, the
 * message at the front of the internal queue is returned. If there are no
 * Messages in the queue then some will be added and one returned.
 *
 * When a message is returned from the queue, it is expected that the encoder
 * will re-add the message, after it has serialized and sent that message. Upon
 * being re-added, the message will be cleaned, i.e. its contents emptied.
 *
 * As such, only a {@link ReUsableMessage} can be added to the message instance.
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class MessageFactory<M extends ReUsableMessage> {

    protected Queue<M> q = new LinkedList();
    protected int max = 100000;
    protected int min = 500;
    protected Class<M> messageType;

    public MessageFactory(Class<M> type) {
        messageType = type;
    }

    public M newMessage() {
        addEmptyMessages();
        return q.poll();
    }

    protected void addEmptyMessages() {
        if (q.size() < min) {
            //add fill the queue up to 90%
            for (int i = 0; i < (max * 0.1); i++) {
                try {
                    q.add(messageType.newInstance());
                } catch (InstantiationException ex) {
                    Logger.getLogger(MessageFactory.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IllegalAccessException ex) {
                    Logger.getLogger(MessageFactory.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    /**
     * Push a message instance back into the queue. The message will first be
     * cleaned.
     *
     * @param msg
     */
    public void addMessage(M msg) {
        if (q.size() < (max * 0.1)) {
            msg.clean();
            q.add(msg);
        }
    }

    public M fromObject(Object o) {
        throw new UnsupportedOperationException("Implement deserializing "
                + "arbitrary objects to message instances");
    }

    public void setMaxQueueSize(int i) {
        max = i;
    }
}
