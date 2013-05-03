package io.higgs.core;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String format) {
        super(format);
    }
}
