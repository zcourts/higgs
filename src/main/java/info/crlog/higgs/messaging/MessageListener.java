package info.crlog.higgs.messaging;

/**
 * While higgs agents such as Broadcaster or radio uses HiggsEventListener Use
 * classes should only be interested in the message received event.
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
public interface MessageListener {

    /**
     * Invoked when a Message is received
     *
     * @param <M extends Message>
     * @param message
     */
    public <M extends Message> void onMessage(M message);
}
