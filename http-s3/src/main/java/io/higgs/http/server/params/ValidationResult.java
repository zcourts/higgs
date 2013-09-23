package io.higgs.http.server.params;

import java.util.HashMap;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class ValidationResult extends HashMap<String, Object> {
    /**
     * @return true if no validation failed
     */
    public boolean isValid() {
        return size() == 0;
    }
}
