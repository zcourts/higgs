package info.crlog.higgs.protocol.boson;

import info.crlog.higgs.messaging.ReUsableMessage;
import info.crlog.higgs.util.StringUtil;
import io.netty.buffer.ChannelBuffer;
import io.netty.buffer.ChannelBuffers;

/**
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class BosonMessage implements ReUsableMessage {

    protected StringUtil util = new StringUtil();
    protected String topic = "";
    protected String content = "";
    private short version = 2;

    public BosonMessage() {
    }

    public BosonMessage(String topic, String content) {
        this.topic = topic;
        this.content = content;
    }

    public BosonMessage(String content) {
        this.content = content;
    }

    public void clean() {
        topic = "";
        content = "";
    }

    public int getMessageSize(ChannelBuffer buffer) {
        // Wait until the protocol version and message size is available
        if (buffer.readableBytes() < 6) {
            return -1;
        }
        version = buffer.readShort(); //protocol version, first 2 bytes
        return buffer.readInt(); //message size, 3rd to 6th bytes, i.e. 32 bits
    }

    public void deserialize(ChannelBuffer buf) {
        //put the protocol version
        version = buf.readShort();
        //get the message length
        buf.readInt();
        //de-serialize the topic
        //get the property key's size
        short topicKeyLength = buf.readShort();
        if (topicKeyLength > 0) //get the property key
        {
            buf.readBytes(topicKeyLength);
        }
        //get the property value's size
        int topicValueLength = buf.readInt();
        if (topicValueLength > 0) //get the property value
        {
            topic = util.getString(buf.readBytes(topicValueLength).array());
        }
        //now de-serialize the message contents
        //get the property key's size
        short valueKeyLength = buf.readShort();
        if (valueKeyLength > 0) //add the property key
        {
            buf.readBytes(valueKeyLength);
        }
        //get the property value's size
        int valueLength = buf.readInt();
        if (valueLength > 0) //get the property value
        {
            content = util.getString(buf.readBytes(valueLength).array());
        }
    }

    public ChannelBuffer serialize() {
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        //put the protocol version
        buf.writeShort(version);
        //serialize topic and conent...
        String topicStr = "topic";
        String contentStr = "content";
        byte[] topicKey = util.getBytes(topicStr);
        byte[] topicContent = util.getBytes(topic);
        byte[] valueKey = util.getBytes(contentStr);
        byte[] valueContent = util.getBytes(content);
        //write the message length
        int messageSize = topicKey.length + topicContent.length
                + valueKey.length + valueContent.length;
        buf.writeInt(messageSize);
        //serialize the topic
        //add the property key's size
        buf.writeShort(topicKey.length);
        //add the property key
        buf.writeBytes(topicKey);
        //add the property value's size
        buf.writeInt(topicContent.length);
        //add the property value
        buf.writeBytes(topicContent);

        //now serialize the message contents
        //add the property key's size
        buf.writeShort(valueKey.length);
        //add the property key
        buf.writeBytes(valueKey);
        //add the property value's size
        buf.writeInt(valueContent.length);
        //add the property value
        buf.writeBytes(valueContent);

        return buf;
    }

    public BosonMessage setContent(String msg) {
        content = msg;
        return this;
    }

    public BosonMessage setTopic(String topic) {
        this.topic = topic;
        return this;
    }

    @Override
    public String toString() {
        return "t:" + topic + "<->c:" + content;
    }

    public String getTopic() {
        return topic;
    }

    public String getContent() {
        return content;
    }
}
