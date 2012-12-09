package com.fillta.higgs.boson.serialization;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class UnsupportedBosonTypeException extends RuntimeException {
    public UnsupportedBosonTypeException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
