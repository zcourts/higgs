package io.higgs.hmq.protocol.serialization;

import io.higgs.hmq.protocol.Signature;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class SignatureDecoder extends ByteToMessageDecoder {
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        //need 13 bytes to get a handshake
        if (in.readableBytes() < 10) {
            return;
        }
        //make sure we're using zmtp/2.0
        if (0xFF != in.getUnsignedByte(in.readerIndex())) {
            out.add(new Signature(false, in));
            return;
        }
        //looking good, now is the least significant bit of the 10th byte a 0?
        int _10thByte = in.getUnsignedByte(in.readerIndex());

        if ((_10thByte & 1) == 0) {
            out.add(new Signature(false, in));
            return;
        }
        out.add(Signature.create(in));
        //once we got the signature we remove this decoder so that the revision and socket type decoder can be next
        ctx.pipeline().remove(this);
    }
}
