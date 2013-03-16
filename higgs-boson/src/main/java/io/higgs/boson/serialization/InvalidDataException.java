package io.higgs.boson.serialization;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class InvalidDataException extends RuntimeException {

    public InvalidDataException(String msg, Throwable c) {
        super(msg, c);
    }
}
