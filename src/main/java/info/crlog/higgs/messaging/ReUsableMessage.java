package info.crlog.higgs.messaging;

/**
 * It is sometimes not necessary to create a new instance of a message
 * implementation. In fact, although new instances are cheap, creating and
 * destroying messages un-necessarily could lead to Garbage collection; which in
 * some cases become a problem for several reasons. Memory is one issue but even
 * worse is a garbage collection cycle that causes the JVM to virtually halt all
 * processing until objects are cleaned etc... A re-usable message is intended
 * to avoid creating a new instance every time. Instead, a "pool" of messages
 * are created in the MessageFactory then, using the methods defined in this
 * interface the message factory "cleans" instances that already exist and those
 * cleaned messages are re-used.
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
public interface ReUsableMessage extends Message {

    /**
     * Implementations are expected to reset their internal fields to the state
     * they would be in if a new instance was created
     */
    public void clean();
}
