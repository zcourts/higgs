package io.higgs.hmq;

import java.io.Closeable;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class Context implements Closeable {

    /**
     * Create a new Socket within this context.
     *
     * @param type the socket type.
     * @return the newly created Socket.
     */
    public ZMQ.Socket socket(int type) {
        return new ZMQ.Socket(this, type);
    }

    /**
     * Create a new Poller within this context, with a default size.
     *
     * @return the newly created Poller.
     * @deprecated use Poller constructor
     */
//        public Poller poller() {
//            return new Poller(this);
//        }
    public void close() {
    }
}