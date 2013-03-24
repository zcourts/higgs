package io.higgs.boson.serialization.kryo;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class KryoException extends RuntimeException {
    public KryoException(String message, Throwable cause) {
        super(message, cause);
    }
}
