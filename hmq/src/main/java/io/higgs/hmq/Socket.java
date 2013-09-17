package io.higgs.hmq;

import org.zeromq.ZMQException;

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class Socket implements Closeable {
    public void close() {
    }

    /**
     * @return the maxMsgSize.
     * @see #setMaxMsgSize(long)
     * @since 3.0.0
     */
    public long getMaxMsgSize() {
        return getLongSockopt(MAXMSGSIZE);
    }

    /**
     * @return the SndHWM.
     * @see #setSndHWM(long)
     * @since 3.0.0
     */
    public long getSndHWM() {
        return getLongSockopt(SNDHWM);
    }

    /**
     * @return the recvHWM period.
     * @see #setRcvHWM(long)
     * @since 3.0.0
     */
    public long getRcvHWM() {
        return getLongSockopt(RCVHWM);
    }

    /**
     * @return the High Water Mark.
     * @see #setHWM(long)
     */
    public long getHWM() {
        return getLongSockopt(HWM);
    }

    /**
     * @return the number of messages to swap at most.
     * @see #setSwap(long)
     */
    public long getSwap() {
        return getLongSockopt(SWAP);
    }

    /**
     * @return the affinity.
     * @see #setAffinity(long)
     */
    public long getAffinity() {
        return getLongSockopt(AFFINITY);
    }

    /**
     * @return the keep alive setting.
     * @see #setTCPKeepAlive(long)
     */
    public long getTCPKeepAliveSetting() {
        return getLongSockopt(KEEPALIVE);
    }

    /**
     * @return the keep alive idle value.
     * @see #setTCPKeepAliveIdle(long)
     */
    public long getTCPKeepAliveIdle() {
        return getLongSockopt(KEEPALIVEIDLE);
    }

    /**
     * @return the keep alive interval.
     * @see #setTCPKeepAliveInterval(long)
     */
    public long getTCPKeepAliveInterval() {
        return getLongSockopt(KEEPALIVEINTVL);
    }

    /**
     * @return the keep alive count.
     * @see #setTCPKeepAliveCount(long)
     */
    public long getTCPKeepAliveCount() {
        return getLongSockopt(KEEPALIVECNT);
    }

    /**
     * @return the Identitiy.
     * @see #setIdentity(byte[])
     */
    public byte[] getIdentity() {
        return getBytesSockopt(IDENTITY);
    }

    /**
     * @return the Rate.
     * @see #setRate(long)
     */
    public long getRate() {
        return getLongSockopt(RATE);
    }

    /**
     * @return the RecoveryIntervall.
     * @see #setRecoveryInterval(long)
     */
    public long getRecoveryInterval() {
        return getLongSockopt(RECOVERY_IVL);
    }

    /**
     * @return the Multicast Loop.
     * @see #setMulticastLoop(boolean)
     */
    public boolean hasMulticastLoop() {
        return getLongSockopt(MCAST_LOOP) != 0;
    }

    /**
     * Sets the time-to-live field in every multicast packet sent from this socket. The default is 1 which means
     * that the multicast packets don't leave the local network.
     *
     * @param mcast_hops
     */
    public void setMulticastHops(long mcast_hops) {
        setLongSockopt(MULTICAST_HOPS, mcast_hops);
    }

    /**
     * @return the Multicast Hops.
     * @see #setMulticastHops(long)
     */
    public long getMulticastHops() {
        return getLongSockopt(MULTICAST_HOPS);
    }

    /**
     * Sets the timeout for receive operation on the socket. If the value is 0, recv will return immediately, with a
     * EAGAIN error if there is no message to receive. If the value is -1, it will block until a message is
     * available. For all other values, it will wait for a message for that amount of time before returning with an
     * EAGAIN error.
     *
     * @param timeout Timeout for receive operation in milliseconds. Default -1 (infinite)
     */
    public void setReceiveTimeOut(int timeout) {
        setLongSockopt(RCVTIMEO, timeout);
    }

    /**
     * @return the Receive Timeout in milliseconds
     * @see #setReceiveTimeOut(long)
     */
    public int getReceiveTimeOut() {
        return (int) getLongSockopt(RCVTIMEO);
    }

    /**
     * Sets the timeout for send operation on the socket. If the value is 0, send will return immediately, with a
     * EAGAIN error if the message cannot be sent. If the value is -1, it will block until the message is sent. For
     * all other values, it will try to send the message for that amount of time before returning with an EAGAIN
     * error.
     *
     * @param timeout Timeout for send operation in milliseconds. Default -1 (infinite)
     */
    public void setSendTimeOut(int timeout) {
        setLongSockopt(SNDTIMEO, timeout);
    }

    /**
     * @return the Send Timeout. in milliseconds
     * @see #setSendTimeOut(long)
     */
    public int getSendTimeOut() {
        return (int) getLongSockopt(SNDTIMEO);
    }

    /**
     * @return the kernel send buffer size.
     * @see #setSendBufferSize(long)
     */
    public long getSendBufferSize() {
        return getLongSockopt(SNDBUF);
    }

    /**
     * @return the kernel receive buffer size.
     * @see #setReceiveBufferSize(long)
     */
    public long getReceiveBufferSize() {
        return getLongSockopt(RCVBUF);
    }

    /**
     * @return the IPv4 only socket.
     * @see #setIPv4only(long)
     */
    public boolean getIPv4Only() {
        return getLongSockopt(IPV4ONLY) == 1;
    }

    /**
     * The 'ZMQ_RCVMORE' option shall return a boolean value indicating if the multi-part message currently being
     * read from the specified 'socket' has more message parts to follow. If there are no message parts to follow or
     * if the message currently being read is not a multi-part message a value of zero shall be returned. Otherwise,
     * a value of 1 shall be returned.
     *
     * @return true if there are more messages to receive.
     */
    public boolean hasReceiveMore() {
        return getLongSockopt(RCVMORE) != 0;
    }

    /**
     * The 'ZMQ_FD' option shall retrieve file descriptor associated with the 0MQ socket. The descriptor can be used
     * to integrate 0MQ socket into an existing event loop. It should never be used for anything else than polling
     * -- such as reading or writing. The descriptor signals edge-triggered IN event when something has happened
     * within the 0MQ socket. It does not necessarily mean that the messages can be read or written. Check
     * ZMQ_EVENTS option to find out whether the 0MQ socket is readable or writeable.
     *
     * @return the underlying file descriptor.
     * @since 2.1.0
     */
    public long getFD() {
        return getLongSockopt(FD);
    }

    /**
     * The 'ZMQ_EVENTS' option shall retrieve event flags for the specified socket. If a message can be read from
     * the socket ZMQ_POLLIN flag is set. If message can be written to the socket ZMQ_POLLOUT flag is set.
     *
     * @return the mask of outstanding events.
     * @since 2.1.0
     */
    public long getEvents() {
        return getLongSockopt(EVENTS);
    }

    /**
     * The 'ZMQ_LINGER' option shall retrieve the period for pending outbound messages to linger in memory after
     * closing the socket. Value of -1 means infinite. Pending messages will be kept until they are fully
     * transferred to the peer. Value of 0 means that all the pending messages are dropped immediately when socket
     * is closed. Positive value means number of milliseconds to keep trying to send the pending messages before
     * discarding them.
     *
     * @param linger the linger period.
     * @since 2.1.0
     */
    public void setLinger(long linger) {
        setLongSockopt(LINGER, linger);
    }

    /**
     * @since 3.0.0
     */
    public void setReconnectIVL(long reconnectIVL) {
        setLongSockopt(RECONNECT_IVL, reconnectIVL);
    }

    /**
     * @since 3.0.0
     */
    public void setBacklog(long backlog) {
        setLongSockopt(BACKLOG, backlog);
    }

    /**
     * @since 3.0.0
     */
    public void setReconnectIVLMax(long reconnectIVLMax) {
        setLongSockopt(RECONNECT_IVL_MAX, reconnectIVLMax);
    }

    /**
     * @since 3.0.0
     */
    public void setMaxMsgSize(long maxMsgSize) {
        setLongSockopt(MAXMSGSIZE, maxMsgSize);
    }

    /**
     * @since 3.0.0
     */
    public void setSndHWM(long sndHWM) {
        setLongSockopt(SNDHWM, sndHWM);
    }

    /**
     * @since 3.0.0
     */
    public void setRcvHWM(long rcvHWM) {
        setLongSockopt(RCVHWM, rcvHWM);
    }

    /**
     * The 'ZMQ_HWM' option shall set the high water mark for the specified 'socket'. The high water mark is a hard
     * limit on the maximum number of outstanding messages 0MQ shall queue in memory for any single peer that the
     * specified 'socket' is communicating with.
     * <p/>
     * If this limit has been reached the socket shall enter an exceptional state and depending on the socket type,
     * 0MQ shall take appropriate action such as blocking or dropping sent messages. Refer to the individual socket
     * descriptions in the man page of zmq_socket[3] for details on the exact action taken for each socket type.
     *
     * @param hwm the number of messages to queue.
     */
    public void setHWM(long hwm) {
        setLongSockopt(HWM, hwm);
    }

    /**
     * Get the Swap. The 'ZMQ_SWAP' option shall set the disk offload (swap) size for the specified 'socket'. A
     * socket which has 'ZMQ_SWAP' set to a non-zero value may exceed its high water mark; in this case outstanding
     * messages shall be offloaded to storage on disk rather than held in memory.
     *
     * @param swap The value of 'ZMQ_SWAP' defines the maximum size of the swap space in bytes.
     */
    public void setSwap(long swap) {
        setLongSockopt(SWAP, swap);
    }

    /**
     * Get the Affinity. The 'ZMQ_AFFINITY' option shall set the I/O thread affinity for newly created connections
     * on the specified 'socket'.
     * <p/>
     * Affinity determines which threads from the 0MQ I/O thread pool associated with the socket's _context_ shall
     * handle newly created connections. A value of zero specifies no affinity, meaning that work shall be
     * distributed fairly among all 0MQ I/O threads in the thread pool. For non-zero values, the lowest bit
     * corresponds to thread 1, second lowest bit to thread 2 and so on. For example, a value of 3 specifies that
     * subsequent connections on 'socket' shall be handled exclusively by I/O threads 1 and 2.
     * <p/>
     * See also in the man page of zmq_init[3] for details on allocating the number of I/O threads for a specific
     * _context_.
     *
     * @param affinity the affinity.
     */
    public void setAffinity(long affinity) {
        setLongSockopt(AFFINITY, affinity);
    }

    /**
     * Override SO_KEEPALIVE socket option (where supported by OS) to enable keep-alive packets for a socket
     * connection. Possible values are -1, 0, 1. The default value -1 will skip all overrides and do the OS default.
     *
     * @param optVal The value of 'ZMQ_TCP_KEEPALIVE' to turn TCP keepalives on (1) or off (0).
     */
    public void setTCPKeepAlive(long optVal) {
    }

    /**
     * Override TCP_KEEPCNT socket option (where supported by OS). The default value -1 will skip all overrides and
     * do the OS default.
     *
     * @param optVal The value of 'ZMQ_TCP_KEEPALIVE_CNT' defines the number of keepalives before death.
     */
    public void setTCPKeepAliveCount(long optVal) {
    }

    /**
     * Override TCP_KEEPINTVL socket option (where supported by OS). The default value -1 will skip all overrides
     * and do the OS default.
     *
     * @param optVal The value of 'ZMQ_TCP_KEEPALIVE_INTVL' defines the interval between keepalives. Unit is OS
     *               dependant.
     */
    public void setTCPKeepAliveInterval(long optVal) {
    }

    /**
     * Override TCP_KEEPCNT (or TCP_KEEPALIVE on some OS) socket option (where supported by OS). The default value
     * -1 will skip all overrides and do the OS default.
     *
     * @param optVal The value of 'ZMQ_TCP_KEEPALIVE_IDLE' defines the interval between the last data packet sent
     *               over the socket and the first keepalive probe. Unit is OS dependant.
     */
    public void setTCPKeepAliveIdle(long optVal) {
    }

    /**
     * The 'ZMQ_IDENTITY' option shall set the identity of the specified 'socket'. Socket identity determines if
     * existing 0MQ infastructure (_message queues_, _forwarding devices_) shall be identified with a specific
     * application and persist across multiple runs of the application.
     * <p/>
     * If the socket has no identity, each run of an application is completely separate from other runs. However,
     * with identity set the socket shall re-use any existing 0MQ infrastructure configured by the previous run(s).
     * Thus the application may receive messages that were sent in the meantime, _message queue_ limits shall be
     * shared with previous run(s) and so on.
     * <p/>
     * Identity should be at least one byte and at most 255 bytes long. Identities starting with binary zero are
     * reserved for use by 0MQ infrastructure.
     *
     * @param identity
     */
    public void setIdentity(byte[] identity) {
        setBytesSockopt(IDENTITY, identity);
    }

    /**
     * The 'ZMQ_SUBSCRIBE' option shall establish a new message filter on a 'ZMQ_SUB' socket. Newly created
     * 'ZMQ_SUB' sockets shall filter out all incoming messages, therefore you should call this option to establish
     * an initial message filter.
     * <p/>
     * An empty 'option_value' of length zero shall subscribe to all incoming messages. A non-empty 'option_value'
     * shall subscribe to all messages beginning with the specified prefix. Mutiple filters may be attached to a
     * single 'ZMQ_SUB' socket, in which case a message shall be accepted if it matches at least one filter.
     *
     * @param topic
     */
    public void subscribe(byte[] topic) {
        setBytesSockopt(SUBSCRIBE, topic);
    }

    /**
     * The 'ZMQ_UNSUBSCRIBE' option shall remove an existing message filter on a 'ZMQ_SUB' socket. The filter
     * specified must match an existing filter previously established with the 'ZMQ_SUBSCRIBE' option. If the socket
     * has several instances of the same filter attached the 'ZMQ_UNSUBSCRIBE' option shall remove only one
     * instance, leaving the rest in place and functional.
     *
     * @param topic
     */
    public void unsubscribe(byte[] topic) {
        setBytesSockopt(UNSUBSCRIBE, topic);
    }

    /**
     * The 'ZMQ_RATE' option shall set the maximum send or receive data rate for multicast transports such as in the
     * man page of zmq_pgm[7] using the specified 'socket'.
     *
     * @param rate
     */
    public void setRate(long rate) {
        setLongSockopt(RATE, rate);
    }

    /**
     * The 'ZMQ_RECOVERY_IVL' option shall set the recovery interval for multicast transports using the specified
     * 'socket'. The recovery interval determines the maximum time in seconds (before version 3.0.0) or milliseconds
     * (version 3.0.0 and after) that a receiver can be absent from a multicast group before unrecoverable data loss
     * will occur.
     * <p/>
     * CAUTION: Exercise care when setting large recovery intervals as the data needed for recovery will be held in
     * memory. For example, a 1 minute recovery interval at a data rate of 1Gbps requires a 7GB in-memory buffer.
     * {Purpose of this Method}
     *
     * @param recovery_ivl
     */
    public void setRecoveryInterval(long recovery_ivl) {
        setLongSockopt(RECOVERY_IVL, recovery_ivl);
    }

    /**
     * The 'ZMQ_MCAST_LOOP' option shall control whether data sent via multicast transports using the specified
     * 'socket' can also be received by the sending host via loopback. A value of zero disables the loopback
     * functionality, while the default value of 1 enables the loopback functionality. Leaving multicast loopback
     * enabled when it is not required can have a negative impact on performance. Where possible, disable
     * 'ZMQ_MCAST_LOOP' in production environments.
     *
     * @param mcast_loop
     */
    public void setMulticastLoop(boolean mcast_loop) {
        setLongSockopt(MCAST_LOOP, mcast_loop ? 1 : 0);
    }

    /**
     * The 'ZMQ_SNDBUF' option shall set the underlying kernel transmit buffer size for the 'socket' to the
     * specified size in bytes. A value of zero means leave the OS default unchanged. For details please refer to
     * your operating system documentation for the 'SO_SNDBUF' socket option.
     *
     * @param sndbuf
     */
    public void setSendBufferSize(long sndbuf) {
        setLongSockopt(SNDBUF, sndbuf);
    }

    /**
     * The 'ZMQ_RCVBUF' option shall set the underlying kernel receive buffer size for the 'socket' to the specified
     * size in bytes. A value of zero means leave the OS default unchanged. For details refer to your operating
     * system documentation for the 'SO_RCVBUF' socket option.
     *
     * @param rcvbuf
     */
    public void setReceiveBufferSize(long rcvbuf) {
        setLongSockopt(RCVBUF, rcvbuf);
    }

    /**
     * The 'ZMQ_IPV4ONLY' option shall set the underlying native socket type. An IPv6 socket lets applications
     * connect to and accept connections from both IPv4 and IPv6 hosts.
     *
     * @param v4only A value of true will use IPv4 sockets, while the value of false will use IPv6 sockets
     */
    public void setIPv4Only(boolean v4only) {
        setLongSockopt(IPV4ONLY, v4only ? 1L : 0L);
    }

    /**
     * Sets the ROUTER socket behavior when an unroutable message is encountered.
     *
     * @param mandatory A value of false is the default and discards the message silently when it cannot be routed.
     *                  A value of true returns an EHOSTUNREACH error code if the message cannot be routed.
     */
    public void setRouterMandatory(boolean mandatory) {
        setLongSockopt(ROUTER_MANDATORY, mandatory ? 1L : 0L);
    }

    /**
     * Sets the XPUB socket behavior on new subscriptions and unsubscriptions.
     *
     * @param verbose A value of false is the default and passes only new subscription messages to upstream.
     *                A value of true passes all subscription messages upstream.
     * @since 3.2.2
     */
    public void setXpubVerbose(boolean verbose) {
        setLongSockopt(XPUB_VERBOSE, verbose ? 1L : 0L);
    }

    /**
     * Bind to network interface. Start listening for new connections.
     *
     * @param addr the endpoint to bind to.
     */
    public native void bind(String addr);

    /**
     * Bind to network interface to a random port. Start listening for new connections.
     *
     * @param addr the endpoint to bind to.
     */
    public int bindToRandomPort(String addr) {
        return bindToRandomPort(addr, 2000, 20000, 100);
    }

    /**
     * Bind to network interface to a random port. Start listening for new connections.
     *
     * @param addr     the endpoint to bind to.
     * @param min_port The minimum port in the range of ports to try.
     */
    public int bindToRandomPort(String addr, int min_port) {
        return bindToRandomPort(addr, min_port, 20000, 100);
    }

    /**
     * Bind to network interface to a random port. Start listening for new connections.
     *
     * @param addr     the endpoint to bind to.
     * @param min_port The minimum port in the range of ports to try.
     * @param max_port The maximum port in the range of ports to try.
     */
    public int bindToRandomPort(String addr, int min_port, int max_port) {
        return bindToRandomPort(addr, min_port, max_port, 100);
    }

    /**
     * Bind to network interface to a random port. Start listening for new connections.
     *
     * @param addr      the endpoint to bind to.
     * @param min_port  The minimum port in the range of ports to try.
     * @param max_port  The maximum port in the range of ports to try.
     * @param max_tries The number of attempt to bind.
     */
    public int bindToRandomPort(String addr, int min_port, int max_port, int max_tries) {
        int port;
        Random rand = new Random();
        for (int i = 0; i < max_tries; i++) {
            port = rand.nextInt(max_port - min_port + 1) + min_port;
            try {
                bind(String.format("%s:%s", addr, port));
                return port;
            } catch (ZMQException e) {
                if (e.getErrorCode() != ZMQ.EADDRINUSE()) {
                    throw e;
                }
                continue;
            }
        }
        throw new ZMQException("Could not bind socket to random port.", (int) ZMQ.EADDRINUSE());
    }

    /**
     * Unbind from network interface. Stop listening for connections.
     *
     * @param addr the endpoint to unbind from.
     */
    public void unbind(String addr);

    /**
     * Connect to remote application.
     *
     * @param addr the endpoint to connect to.
     */
    public void connect(String addr);

    /**
     * Disconnect from a remote application.
     *
     * @param addr the endpoint to disconnect from.
     */
    public void disconnect(String addr);

    /**
     * Send a message.
     *
     * @param msg    the message to send, as an array of bytes.
     * @param offset the offset of the message to send.
     * @param flags  the flags to apply to the send operation.
     * @return true if send was successful, false otherwise.
     */
    public boolean send(byte[] msg, int offset, int flags) {
        return send(msg, offset, msg.length, flags);
    }

    /**
     * @param msg
     * @param offset
     * @param len
     * @param flags
     * @return
     */
    public native boolean send(byte[] msg, int offset, int len, int flags);

    /**
     * Perform a zero copy send. The buffer must be allocated using ByteBuffer.allocateDirect
     *
     * @param buffer
     * @param len
     * @param flags
     * @return
     */
    public boolean sendZeroCopy(ByteBuffer buffer, int len, int flags);

    /**
     * Send a message.
     *
     * @param msg   the message to send, as an array of bytes.
     * @param flags the flags to apply to the send operation.
     * @return true if send was successful, false otherwise.
     */
    public boolean send(byte[] msg, int flags) {
        return send(msg, 0, msg.length, flags);
    }

    /**
     * Send a String.
     *
     * @param msg the message to send, as a String.
     * @return true if send was successful, false otherwise.
     */

    public boolean send(String msg) {
        byte[] b = msg.getBytes();
        return send(b, 0, b.length, 0);
    }

    /**
     * Send a String.
     *
     * @param msg the message to send, as a String.
     * @return true if send was successful, false otherwise.
     */

    public boolean sendMore(String msg) {
        byte[] b = msg.getBytes();
        return send(b, 0, b.length, SNDMORE);
    }

    /**
     * Send a String.
     *
     * @param msg   the message to send, as a String.
     * @param flags the flags to apply to the send operation.
     * @return true if send was successful, false otherwise.
     */

    public boolean send(String msg, int flags) {
        byte[] b = msg.getBytes();
        return send(b, 0, b.length, flags);
    }

    /**
     * Send a message
     *
     * @param bb    ByteBuffer payload
     * @param flags the flags to apply to the send operation
     * @return the number of bytes sent
     */
    public int sendByteBuffer(ByteBuffer bb, int flags);

    /**
     * Receive a message.
     *
     * @param flags the flags to apply to the receive operation.
     * @return the message received, as an array of bytes; null on error.
     */
    public byte[] recv(int flags);

    /**
     * Receive a message in to a specified buffer.
     *
     * @param buffer byte[] to copy zmq message payload in to.
     * @param offset offset in buffer to write data
     * @param len    max bytes to write to buffer. If len is smaller than the incoming message size,
     *               the message will
     *               be truncated.
     * @param flags  the flags to apply to the receive operation.
     * @return the number of bytes read, -1 on error
     */
    public int recv(byte[] buffer, int offset, int len, int flags);

    /**
     * Zero copy recv
     *
     * @param buffer
     * @param len
     * @param flags
     * @return bytes read, -1 on error
     */
    public native int recvZeroCopy(ByteBuffer buffer, int len, int flags);

    /**
     * Receive a message.
     *
     * @return the message received, as an array of bytes; null on error.
     */
    public final byte[] recv() {
        return recv(0);
    }

    /**
     * Receive a message as a String.
     *
     * @return the message received, as a String; null on error.
     */
    public String recvStr() {
        return recvStr(0);
    }

    /**
     * Receive a message as a String.
     *
     * @param flags the flags to apply to the receive operation.
     * @return the message received, as a String; null on error.
     */

    public String recvStr(int flags) {
        byte[] data = recv(flags);

        if (data == null) {
            return null;
        }

        return new String(data);
    }

    /**
     * Receive a message
     *
     * @param buffer
     * @param flags
     * @return bytes read, -1 on error
     */
    public int recvByteBuffer(ByteBuffer buffer, int flags);

    /**
     * Class constructor.
     *
     * @param context a 0MQ context previously created.
     * @param type    the socket type.
     */
    protected Socket(Context context, int type) {
        // We keep a local handle to context so that
        // garbage collection won't be too greedy on it.
        this.context = context;
        construct(context, type);
    }

    /**
     * Initialize the JNI interface
     */
    protected void construct(Context ctx, int type);

    /**
     * Free all resources used by JNI interface.
     */
    protected void destroy();

    /**
     * Get the socket option value, as a long.
     *
     * @param option ID of the option to set.
     * @return The socket option value (as a long).
     */
    protected long getLongSockopt(int option);

    /**
     * Get the socket option value, as a byte array.
     *
     * @param option ID of the option to set.
     * @return The socket option value (as a byte array).
     */
    protected byte[] getBytesSockopt(int option);

    /**
     * Set the socket option value, given as a long.
     *
     * @param option ID of the option to set.
     * @param optval value (as a long) to set the option to.
     */
    protected void setLongSockopt(int option, long optval);

    /**
     * Set the socket option value, given as a byte array.
     *
     * @param option ID of the option to set.
     * @param optval value (as a byte array) to set the option to.
     */
    protected void setBytesSockopt(int option, byte[] optval);

    /**
     * Get the underlying socket handle. This is private because it is only accessed from JNI, where Java access
     * controls are ignored.
     *
     * @return the internal 0MQ socket handle.
     */
    private long getSocketHandle() {
        return this.socketHandle;
    }

    /**
     * Opaque data used by JNI driver.
     */
    private long socketHandle;
    private Context context = null;
    // private Constants use the appropriate setter instead.
    private static final int HWM = 1;
    // public static final int LWM = 2; // No longer supported
    private static final int SWAP = 3;
    private static final int AFFINITY = 4;
    private static final int IDENTITY = 5;
    private static final int SUBSCRIBE = 6;
    private static final int UNSUBSCRIBE = 7;
    private static final int RATE = 8;
    private static final int RECOVERY_IVL = 9;
    private static final int MCAST_LOOP = 10;
    private static final int SNDBUF = 11;
    private static final int RCVBUF = 12;
    private static final int RCVMORE = 13;
    private static final int FD = 14;
    private static final int EVENTS = 15;
    private static final int TYPE = 16;
    private static final int LINGER = 17;
    private static final int RECONNECT_IVL = 18;
    private static final int BACKLOG = 19;
    private static final int RECONNECT_IVL_MAX = 21;
    private static final int MAXMSGSIZE = 22;
    private static final int SNDHWM = 23;
    private static final int RCVHWM = 24;
    private static final int MULTICAST_HOPS = 25;
    private static final int RCVTIMEO = 27;
    private static final int SNDTIMEO = 28;
    private static final int IPV4ONLY = 31;
    private static final int ROUTER_MANDATORY = 33;
    private static final int KEEPALIVE = 34;
    private static final int KEEPALIVECNT = 35;
    private static final int KEEPALIVEIDLE = 36;
    private static final int KEEPALIVEINTVL = 37;
    private static final int XPUB_VERBOSE = 40;

}