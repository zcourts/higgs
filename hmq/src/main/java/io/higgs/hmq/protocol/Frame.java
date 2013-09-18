package io.higgs.hmq.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * @author Courtney Robinson <courtney.robinson@datasift.com>
 */
public class Frame {
    private ByteBuf data = Unpooled.buffer();
    private Command command;
    private boolean commandFrame;

    public Frame(Command command) {
        this.commandFrame = true;
        this.command = command;
    }

    public Frame(ByteBuf msg) {
        this.commandFrame = false;
        this.data.release();
        this.data = msg;
    }

    /**
     * Given a byte b set the bit at the given position to 1
     *
     * @param b        the byte to manipulate
     * @param position the bit position
     * @return the modified byte
     */
    private byte setBit(byte b, int position) {
        return b |= 1 << position;
    }

    /**
     * Given a byte b set the bit at the given position to 0
     *
     * @param b        the byte to manipulate
     * @param position the bit position
     * @return the modified byte
     */
    private byte unsetBit(byte b, int position) {
        return b &= ~(1 << position);
    }

    public ByteBuf encode() {
        if (isCommandFrame()) {
            data.writeByte(command.val());
        } else {

        }
        return data;
    }

    public boolean isCommandFrame() {
        return commandFrame;
    }

    public static enum Command {
        SUBSCRIBE((byte) 0x01),
        UNSUBSCRIBE((byte) 0x00);
        private byte val;

        private Command(byte c) {
            val = c;
        }

        public byte val() {
            return val;
        }
    }
}
