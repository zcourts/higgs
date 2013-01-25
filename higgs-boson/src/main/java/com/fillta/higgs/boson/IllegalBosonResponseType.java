package com.fillta.higgs.boson;

public class IllegalBosonResponseType extends RuntimeException {
    /**
     * When a server responds with an object, if the client expects a different type
     * this field will be an instance of the object returned. The client can determine
     * what to do with it.
     */
    public final Object param;

    public IllegalBosonResponseType(Object param, Throwable cause) {
        super("Invalid response", cause);
        this.param = param;
    }
}
