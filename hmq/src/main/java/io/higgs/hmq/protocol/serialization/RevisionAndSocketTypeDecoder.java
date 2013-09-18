package io.higgs.hmq.protocol.serialization;

import io.higgs.hmq.protocol.Identity;
import io.higgs.hmq.protocol.Socket;
import io.higgs.hmq.protocol.SocketType;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class RevisionAndSocketTypeDecoder extends ByteToMessageDecoder {
    private boolean identitySent;
    private Socket socket;

    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (!identitySent) {
            //need 2 bytes to get the revision and socket type
            if (in.readableBytes() < 2) {
                return;
            }
            int revision = in.readUnsignedByte();
            SocketType socketType = SocketType.fromByte((byte) in.readUnsignedByte());

            socket = new Socket(revision, socketType, ctx);
            //send the identity info
            ctx.channel().writeAndFlush(new Identity());
            identitySent = true;
        } else {
            //from reverse engineering it seems the final short is echoed back which is two bytes
            if (in.readableBytes() < 2) {
                return;
            }
            byte a = in.readByte(); //ignored
            byte b = in.readByte(); //ignored
            //send socket to handler, handshake is complete can start sending messages
            out.add(socket);
            //once we got the signature we remove this decoder
            ctx.pipeline().remove(this);
        }
    }
}
