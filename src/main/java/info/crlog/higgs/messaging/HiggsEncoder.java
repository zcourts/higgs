package info.crlog.higgs.messaging;

import io.netty.buffer.ChannelBuffer;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.oneone.OneToOneEncoder;

/**
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class HiggsEncoder extends OneToOneEncoder {

    protected MessageFactory factory;

    public HiggsEncoder(MessageFactory factory) {
        this.factory = factory;
    }

    @Override
    protected Object encode(ChannelHandlerContext chc, Channel chnl, Object o) throws Exception {
        // Convert to a BosonMessage
        ReUsableMessage msg = null;
        if (o instanceof ReUsableMessage) {
            msg = (ReUsableMessage) o;
        } else {
            msg = factory.fromObject(o);
        }
        //get the serialized byte array
        ChannelBuffer data = msg.serialize();
        //return it to the factory
        factory.addMessage(msg);
        return data;//and return the serialized data to be sent to clients
    }
}
