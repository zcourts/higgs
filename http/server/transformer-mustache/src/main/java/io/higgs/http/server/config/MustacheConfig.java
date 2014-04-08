package io.higgs.http.server.config;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class MustacheConfig {
    public int priority = 1;
    /**
     * The directory to check for mustache templates
     */
    public String directory = "templates";
    /**
     * if true then if a response object is a map each key in the map becomes the name of a variable in the
     * mustache scope with it's value set to the map's value for that key
     */
    public boolean extract_values_from_maps = true;
    /**
     * If true then when an object is returned that is not a collection or primitive the fields names in the object
     * become variables in the mustache scope with the value of the variable being that of the field
     */
    public boolean extract_pojo_fields = true;
    /**
     * if the template name given to the template annotation doesn't have a file extension this is appended
     */
    public String template = ".mustache";
}
