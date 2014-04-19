package io.higgs.http.server.config;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class HandlebarsConfig {
    public int priority = 1;
    /**
     * The directory to check for handlebars templates
     */
    public String directory = "templates";
    /**
     * if the template name given to the template annotation doesn't have a file extension this is appended
     */
    public String template = ".hbs";

    public boolean cache_templates = true;

    public boolean enable_jackson_helper = true;
    public boolean enable_markdown_helper = true;
    public boolean enable_humanize_helper = true;
}
