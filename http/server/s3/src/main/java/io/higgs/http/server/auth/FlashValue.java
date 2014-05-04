package io.higgs.http.server.auth;

import java.io.Serializable;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class FlashValue implements Serializable {

    private final Object value;

    public FlashValue(Object value) {
        this.value = value;
    }

    public Object getValue() {
        return value;
    }
}
