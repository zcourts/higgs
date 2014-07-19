package io.higgs.http.server;

import io.higgs.http.server.params.Validator;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class MethodParam {
    private String name;
    private boolean queryParam;
    private boolean pathParam;
    private boolean formParam;
    private boolean headerParam;
    private boolean cookieParam;
    private boolean validationRequired;
    /**
     * The class type of the parameter
     */
    private Class<?> parameterType;
    private Validator validator;
    /**
     * The index/position of the parameter in the method's parameter list
     */
    private int position;
    private boolean sessionParam;

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public boolean isNamed() {
        return name != null;
    }

    public Validator getValidator() {
        return validator;
    }

    public <T extends Validator> void setValidator(T validator) {
        this.validator = validator;
    }

    public boolean isValidationRequired() {
        return validationRequired;
    }

    public void setValidationRequired(boolean validationRequired) {
        this.validationRequired = validationRequired;
    }

    public boolean isQueryParam() {
        return queryParam;
    }

    public void setQueryParam(final boolean queryParam) {
        this.queryParam = queryParam;
    }

    public boolean isPathParam() {
        return pathParam;
    }

    public void setPathParam(final boolean pathParam) {
        this.pathParam = pathParam;
    }

    public boolean isFormParam() {
        return formParam;
    }

    public void setFormParam(final boolean formParam) {
        this.formParam = formParam;
    }

    public boolean isHeaderParam() {
        return headerParam;
    }

    public void setHeaderParam(final boolean headerParam) {
        this.headerParam = headerParam;
    }

    public boolean isCookieParam() {
        return cookieParam;
    }

    public void setCookieParam(final boolean cookieParam) {
        this.cookieParam = cookieParam;
    }

    public String toString() {
        return "MethodParam{" +
                "methodClass=" + parameterType.getName() +
                ", name='" + name + '\'' +
                ", queryParam=" + queryParam +
                ", pathParam=" + pathParam +
                ", formParam=" + formParam +
                ", headerParam=" + headerParam +
                ", cookieParam=" + cookieParam +
                '}';
    }

    public Class<?> getParameterType() {
        return parameterType;
    }

    public void setParameterType(Class<?> parameterType) {
        this.parameterType = parameterType;
    }

    /**
     * @return The position/index of the parameter in the method's formal parameter list
     */
    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public boolean isSessionParam() {
        return sessionParam;
    }

    public void setSessionParam(boolean sessionParam) {
        this.sessionParam = sessionParam;
    }
}

