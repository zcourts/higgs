package io.higgs.http.server.params;

import io.higgs.http.server.MethodParam;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public interface Validator {
    boolean isValid(Object value);

    String getValidationMessage(MethodParam param);
}
