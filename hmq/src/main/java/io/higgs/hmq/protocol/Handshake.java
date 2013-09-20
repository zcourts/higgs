package io.higgs.hmq.protocol;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class Handshake {
    //TODO: could be made more efficient by creating public static final byte array to represent each possible socket
    // type's handshake, instead of generating the byte array every time for each socket
    private final byte[] data;

    public Handshake(SocketType socket) {
        //%xFF 8%x00 %x7F = signature
        //0x01 = revision = ZMTP/2.0
        //socket.value() = socket type, i.e. PUB, SUB etc
        //0x00 0x00 == identity
        data = new byte[]{ (byte) 0xFF, 0, 0, 0, 0, 0, 0, 0, 0, 0x7F, 0x01, socket.value(), 0x00, 0x00 };
    }

    public byte[] data() {
        return data;
    }
}
