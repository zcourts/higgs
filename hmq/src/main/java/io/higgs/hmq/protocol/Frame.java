package io.higgs.hmq.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * @author Courtney Robinson <courtney.robinson@datasift.com>
 */
public class Frame {
    private ByteBuf data = Unpooled.buffer();
    private Command command;
    private String commandText;
    private boolean commandFrame;

    public Frame(Command command, String body) {
        this.commandFrame = true;
        this.command = command;
        commandText = body;
    }

    public Frame(ByteBuf msg) {
        this.commandFrame = false;
        this.data.release();
        this.data = msg;
    }

    public ByteBuf encode() {
        if (isCommandFrame()) {
            byte[] body = this.commandText.getBytes();
            data.writeByte(0); //flag = no more frames to follow
            data.writeByte(body.length + 1); //length = command body + command
            data.writeByte(command.val()); //command
            data.writeBytes(body); //body
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
