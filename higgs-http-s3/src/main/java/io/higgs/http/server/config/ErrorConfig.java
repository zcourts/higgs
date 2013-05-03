package io.higgs.http.server.config;

import io.higgs.http.server.HttpTemplate;

import java.util.ArrayList;
import java.util.List;

public class ErrorConfig {
    public List<HttpTemplate> templates = new ArrayList<>();
    /**
     * If Higgs provides HTTP error templates that are not explicitly set by the user then they are used
     * if this is true
     */
    public boolean addDefaults = true;
    private HttpTemplate a404;

    public HttpTemplate get404() {
        for (HttpTemplate t : templates) {
            if (t.getCode() == 404) {
                return t;
            }
        }
        return null;
    }
}
