package io.higgs.hmq.protocol;

/**
 * @author Courtney Robinson <courtney.robinson@datasift.com>
 */
public class IllegalFrameSizeException extends RuntimeException {
    public IllegalFrameSizeException(String msg) {
        super(msg);
    }
}
