package io.higgs.hmq.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.nio.charset.Charset;

/**
 * @author Courtney Robinson <courtney.robinson@datasift.com>
 */
public class Message {
    private final Charset utf8 = Charset.forName("UTF-8");
    private ByteBuf data = Unpooled.buffer();
    private Command command;
    private String commandText;
    private boolean commandFrame;

    public Message(Command command, String body) {
        this.commandFrame = true;
        this.command = command;
        commandText = body;
    }

    public Message(ByteBuf contents) {
        this.data = contents;
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

    public ByteBuf contents() {
        return data;
    }

    @Override
    public String toString() {
        return "Message{data=" + data.toString(utf8) + '}';
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
