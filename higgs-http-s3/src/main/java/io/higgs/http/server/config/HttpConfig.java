package io.higgs.http.server.config;

import io.higgs.core.ServerConfig;
import io.higgs.http.server.protocol.HttpMethod;

import java.util.HashMap;
import java.util.Map;

public class HttpConfig extends ServerConfig {
    public TemplateConfig template_config = new TemplateConfig();
    public FilesConfig files = new FilesConfig();
    public ErrorConfig error_templates = new ErrorConfig();
    //
    private Map<Integer, HttpMethod> errors = new HashMap<>();

    public HttpConfig() {
    }
}
