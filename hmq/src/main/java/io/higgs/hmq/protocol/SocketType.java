package io.higgs.hmq.protocol;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public enum SocketType {
    PAIR((byte) 0x00),
    PUB((byte) 0x01),
    SUB((byte) 0x02),
    REQ((byte) 0x03),
    REP((byte) 0x04),
    DEALER((byte) 0x05),
    ROUTER((byte) 0x06),
    PULL((byte) 0x07),
    PUSH((byte) 0x08);
    private byte val;

    SocketType(byte value) {
        val = value;
    }

    public static SocketType fromByte(byte b) {
        switch (b) {
            case 0x00:
                return PAIR;
            case 0x01:
                return PUB;
            case 0x02:
                return SUB;
            case 0x03:
                return REQ;
            case 0x04:
                return REP;
            case 0x05:
                return DEALER;
            case 0x06:
                return ROUTER;
            case 0x07:
                return PULL;
            case 0x08:
                return PUSH;
        }
        throw new IllegalArgumentException(String.format("Value %s, is not a valid socket type ", b));
    }

    public byte value() {
        return val;
    }

    public boolean compatible(SocketType type) {
        if (type == null) {
            return false;
        }
        switch (type) {
            case PUSH:
                //if remote type == push this.val must be pull
                return PULL.value() == val;
            case PULL:
                return PUSH.value() == val;
            case PUB:
                return SUB.value() == val;
            case SUB:
                return PUB.value() == val;
            default:
                throw new UnsupportedOperationException("Only push/pull and pub/sub are supported");
        }
    }
}
