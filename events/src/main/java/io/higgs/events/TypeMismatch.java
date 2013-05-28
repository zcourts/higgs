package io.higgs.events;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class TypeMismatch {
    private final ClassCastException exception;

    public TypeMismatch(ClassCastException cce) {
        exception = cce;
    }
}
