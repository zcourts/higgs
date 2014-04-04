package io.higgs.http.server.params;

import java.util.HashMap;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class ValidationResult extends HashMap<String, Object> {
    private boolean valid = true;

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    /**
     * Mark this validation as invalid
     */
    public void invalid() {
        this.valid = false;
    }

    /**
     * @return true if no validation failed
     */
    public boolean isValid() {
        return valid;
    }

    @Override
    public String toString() {
        return "ValidationResult{" +
                "valid=" + valid +
                '}';
    }
}
