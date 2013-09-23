package io.higgs.http.server.params;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class IllegalValidatorException extends RuntimeException {
    public IllegalValidatorException(String s, Throwable e) {
        super(s, e);
    }
}
