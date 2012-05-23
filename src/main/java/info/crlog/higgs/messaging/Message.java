package info.crlog.higgs.messaging;

import io.netty.buffer.ChannelBuffer;

/**
 * A Message is key to the Higgs library. It allows implementations to define
 * their own protocol by implementing the methods in this interface. This
 * simplifies messaging by providing a higher level abstraction from the lower
 * level networking APIs
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
public interface Message {

    /**
     * When the decoder gets data off the network it will repeatedly call this
     * method until it returns a number greater than 0.
     *
     * The number returned should be how many bytes the decoder needs to read
     * off the network for it to have a "complete" message.
     *
     * Until the size of the message is known, this method should return -1.
     * Doing so tells the decoder that the ChannelBuffer does not yet have
     * enough data to determine the size of the message.
     *
     * @param buf A channel buffer with all the data currently available.
     * Implementations are expected to read from this buffer, determine how many
     * bytes this message should contain and return that size as specified
     * above.
     * @return -1 until the size of the message is know or an integer greater
     * than 0 which represents the size of the current message.
     */
    public int getMessageSize(ChannelBuffer buf);

    /**
     * This method is invoked by the decoder once
     * {@link getMessageSize(ChannelBuffer)} has returned a valid message size
     * and the channel buffer has been populated with as much data.
     * Implementations are expected to read from the provided channel buffer and
     * populate its internal fields
     *
     * @param buf The channel buffer containing the raw bytes of the message to
     * be de-serialized
     */
    public void deserialize(ChannelBuffer buf);

    /**
     * When a message is sent, it is first serialized to a series of bytes and
     * then sent to its intended recipients This method is responsible for
     * converting its internal fields to bytes that can be decoded to the same
     * contents by all recipients
     *
     * @return A Channel buffer which whose contents is sent to recipients
     */
    public ChannelBuffer serialize();
}
