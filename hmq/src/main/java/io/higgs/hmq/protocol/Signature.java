package io.higgs.hmq.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * @author Courtney Robinson <courtney.robinson@datasift.com>
 */
public class Signature {
    public static final byte[] SIGNATURE = new byte[10];
    private final boolean valid;

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

    private final ByteBuf buf;

    public Signature(boolean valid, ByteBuf buf) {
        this.valid = valid;
        this.buf = buf;
    }

    /**
     * Compares the signature  for zmtp/2.0
     * only the first and 10th byte are significant.
     * The first byte must be 0xFF and the 10th must be 0x7F
     *
     * @return true if a valid zmtp/2.0 signature is received
     */
    public static boolean matches(ByteBuf buf) {
        if (buf.readableBytes() < 10) {
            return false;
        }
        //first byte must be 0xFF
        if (buf.readUnsignedByte() != 0xFF) {
            return false;
        }
        //advance the read index by 8 bytes
        buf.readerIndex(buf.readerIndex() + 8);
        //the 10th byte must be 0x7F
        if (buf.readUnsignedByte() != 0x7F) {
            return false;
        }
        return true;
    }

    public static Signature create(ByteBuf buf) {
        return new Signature(matches(buf), buf);
    }

    public static byte[] data() {
        return SIGNATURE;
    }

    public static Signature fixed() {
        return new Signature(true, Unpooled.wrappedBuffer(SIGNATURE));
    }

    public boolean isValid() {
        return valid;
    }

    /**
     * @return The buffer that was used to create this signature
     */
    public ByteBuf rawBuffer() {
        return buf;
    }
}
