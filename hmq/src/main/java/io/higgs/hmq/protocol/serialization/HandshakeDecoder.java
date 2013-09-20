package io.higgs.hmq.protocol.serialization;

import io.higgs.hmq.protocol.Socket;
import io.higgs.hmq.protocol.SocketType;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class HandshakeDecoder extends ByteToMessageDecoder {
    private final SocketType type;
    private Logger log = LoggerFactory.getLogger(getClass());
    private boolean decoded;

    public HandshakeDecoder(SocketType type) {
        this.type = type;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() < 14 || decoded) {
            return;//handshake needs 14 bytes minimum
        }
        //first byte must be 0xFF
        if (in.getUnsignedByte(0) != 0xFF) {
            log.error(String.format("Invalid signature received 0xFF wasn't the first byte"));
            ctx.channel().close();
            return;
        }
        //the 10th byte must be 0x7F
        if (in.getUnsignedByte(9) != 0x7F) {
            log.error(String.format("Invalid signature received 0x7F wasn't the 10th byte"));
            ctx.channel().close();
            return;
        }
        short revision = in.getUnsignedByte(10); //11th byte
        SocketType socketType = SocketType.fromByte((byte) in.getUnsignedByte(11)); //12th byte
        if (!type.compatible(socketType)) {
            log.error(String.format("Incompatible socket types local type = %s remote = %s", type, socketType));
            ctx.channel().close();
            return;
        }
        out.add(new Socket(type, socketType, ctx));
        //advance the reader index to the 14th byte which is the length of the entire handshake
        in.readBytes(new byte[14]);
        //remove from pipeline, handshake is only done once
        ctx.pipeline().remove(this);
        decoded = true;
    }
}
