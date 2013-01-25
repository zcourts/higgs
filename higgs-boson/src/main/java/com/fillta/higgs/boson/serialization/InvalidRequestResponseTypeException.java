package com.fillta.higgs.boson.serialization;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class InvalidRequestResponseTypeException extends RuntimeException {
    public InvalidRequestResponseTypeException(String msg, Throwable c) {
        super(msg, c);
    }
}
