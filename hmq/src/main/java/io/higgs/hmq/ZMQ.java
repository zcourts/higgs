package io.higgs.hmq;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class ZMQ {
    // Values for flags in Socket's send and recv functions.
    /**
     * Socket flag to indicate a nonblocking send or recv mode.
     */
    public static final int NOBLOCK = 1;
    public static final int DONTWAIT = 1;
    /**
     * Socket flag to indicate that more message parts are coming.
     */
    public static final int SNDMORE = 2;

    // Socket types, used when creating a Socket.
    /**
     * Flag to specify a PUB socket, receiving side must be a SUB or XSUB.
     */
    public static final int PUB = 1;
    /**
     * Flag to specify the receiving part of the PUB or XPUB socket.
     */
    public static final int SUB = 2;
    /**
     * Flag to specify the receiving part of a PUSH socket.
     */
    public static final int PULL = 7;
    /**
     * Flag to specify a PUSH socket, receiving side must be a PULL.
     */
    public static final int PUSH = 8;

    /**
     * Create a new Context.
     *
     * @return the Context
     */
    public static Context context() {
        return context(1);
    }

    /**
     * Create a new Context.
     *
     * @param ioThreads IGNORED
     * @return the Context
     */
    public static Context context(int ioThreads) {
        return new Context();
    }

    //just to keep API compatibility
    public static class Context extends io.higgs.hmq.Context {
    }

    public static class Socket extends io.higgs.hmq.Socket {

        /**
         * Class constructor.
         *
         * @param context a 0MQ context previously created.
         * @param type    the socket type.
         */
        protected Socket(io.higgs.hmq.Context context, int type) {
            super(context, type);
        }
    }
}
