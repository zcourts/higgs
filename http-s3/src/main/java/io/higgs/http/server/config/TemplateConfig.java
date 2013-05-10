package io.higgs.http.server.config;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class TemplateConfig {

    public boolean cacheable = true;
    /**
     * 24hrs by default
     * Max time to cache templates for (in Milliseconds)
     */
    public long cache_age_ms = 86400000;
    /**
     * Char encoding used when reading templates
     */
    public String character_encoding = "utf-8";
    public String suffix = ".html";
    /**
     * Sets a new (optional) prefix to be added to all template names in order to convert template names into
     * resource names.
     */
    public String prefix = "templates/";
    public Integer classLoader_resolver_order = 1;
    public Integer fileResolver_order = 2;
    public Integer url_resolver_order = 3;
    public boolean convert_map_responses_to_key_value_pairs = true;
    public boolean convert_pojo_responses_to_key_value_pairs = true;
    public boolean auto_initialize_thymeleaf = true;
    public boolean determine_language_from_accept_header = true;
    public String auto_parse_extensions = "html,htm";

    public String template_mode = "LEGACYHTML5";
}
