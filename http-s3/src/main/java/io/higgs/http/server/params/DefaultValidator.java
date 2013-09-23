package io.higgs.http.server.params;

import io.higgs.http.server.MethodParam;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class DefaultValidator implements Validator {
    @Override
    public boolean isValid(Object value) {
        //value must not be null, and if value is a string it cannot be empty
        return value != null && (!(value instanceof String) || !((String) value).isEmpty());
    }

    @Override
    public String getValidationMessage(MethodParam param) {
        return String.format("%s is required", param.getName());
    }

}
