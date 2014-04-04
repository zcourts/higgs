package io.higgs.http.server.params;

/**
 * A required parameter is any injectable parameter which must pass some validation test in order to be considered valid
 * The validation test is specified by {@link valid}. If no validation test is provided the parameter will be marked
 * as valid ONLY IF  it is not null and if it is a string it must not be empty. Those are the two default validations
 * done anything else must be done with a custom {@link Validator}
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class RequiredParam<T> {
    protected T value;
    protected boolean valid;

    public RequiredParam(T value, boolean valid) {
        this.value = value;
        this.valid = valid;
    }

    public T getValue() {
        return value;
    }

    public boolean isValid() {
        return valid;
    }

    @Override
    public String toString() {
        return "RequiredParam{" +
                "valid=" + valid +
                ", value=" + value +
                '}';
    }
}
