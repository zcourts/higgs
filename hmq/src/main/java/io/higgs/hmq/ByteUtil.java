package io.higgs.hmq;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class ByteUtil {
    /**
     * Given a byte b set the bit at the given position to 1
     *
     * @param b        the byte to manipulate
     * @param position the bit position
     * @return the modified byte
     */
    public static byte setBit(byte b, int position) {
        return b |= 1 << position;
    }

    /**
     * Given a byte b set the bit at the given position to 0
     *
     * @param b        the byte to manipulate
     * @param position the bit position
     * @return the modified byte
     */
    public static byte unsetBit(byte b, int position) {
        return b &= ~(1 << position);
    }

    /**
     * Gets the value of the bit at the given position i.e. 1 or 0
     *
     * @param b        the byte to check
     * @param position the position to check
     * @return 1 if set, 0 otherwise
     */
    public static byte getBit(byte b, int position) {
        return (byte) ((b >> position) & 1);
    }

    /**
     * Check if the bit at the given position is set to 1
     *
     * @param b        the byte to check
     * @param position the position in the byte
     * @return true if the bit at the given position is == 1, false otherwise
     */
    public static boolean isBitSet(byte b, int position) {
        return getBit(b, position) == 1;
    }
}
