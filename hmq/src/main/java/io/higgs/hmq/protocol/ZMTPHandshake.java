package io.higgs.hmq.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * A handshake/greeting for zmtp 2.0 is defined as
 * zmtp        = *connection
 * <p/>
 * connection  = greeting *message
 * <p/>
 * greeting    = signature revision socket-type identity
 * signature   = %xFF 8%x00 %x7F
 * revision    = %x01
 * <p/>
 * socket-type = PAIR | PUB | SUB | REQ | REP | DEALER | ROUTER | PULL | PUSH
 * PAIR        = %X00
 * PUB         = %X01
 * SUB         = %X02
 * REQ         = %X03
 * REP         = %X04
 * DEALER      = %X05
 * ROUTER      = %X06
 * PULL        = %X07
 * PUSH        = %X08
 * <p/>
 * identity    = final-short body
 * final-short = %x00 OCTET
 * body        = *OCTET
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class ZMTPHandshake {
    public static final byte[] SIGNATURE = new byte[9];
    public static final byte REVISION = 0x01;
    public static final byte FINAL_SHORT = 0x00;

    static {
        //signature   = %xFF 8%x00 %x7F
        ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0xFF);
        buf.writeByte(0x00); //1
        buf.writeByte(0x00); //2
        buf.writeByte(0x00); //3
        buf.writeByte(0x00); //4
        buf.writeByte(0x00); //5
        buf.writeByte(0x00); //6
        buf.writeByte(0x00); //7
        buf.writeByte(0x00); //8
        buf.writeByte(0x7F);
        buf.readBytes(SIGNATURE);
        buf.release();
    }

    /**
     * Generate a greeting for one of the possible socket types
     *
     * @param type one of {@link SocketType}
     * @return a series of bytes representing the greeting appropriate for the given {@link SocketType}
     */
    public static byte[] greeting(SocketType type) {
        ByteBuf buf = Unpooled.buffer();
        //greeting = signature revision socket-type identity
        buf.writeBytes(SIGNATURE);
        buf.writeByte(REVISION);
        buf.writeByte(type.getValue());
//        identity    = final-short body
//        final-short = %x00 OCTET
//        body        = *OCTET
        buf.writeByte(FINAL_SHORT);
        //body is 0 or more bytes so just leave it as is
        byte[] greeting = new byte[buf.writerIndex()];
        buf.readBytes(greeting);
        return greeting;
    }
}
