package io.higgs.core;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A resource path is the location where an end point is accessible.
 * For e.g. test/{param:[a-z0-9]} means a resource is available at /test/var
 * where var can be any letter a-z or number 0-9 AND param is accessible by the method
 * to be injected on invocation.
 * A resource's path can have any number of parameters. Parameters are contained withing curly braces {},
 * parameter names are any valid string and parameters can optionally have a regex pattern.
 * If a regex pattern is available it is separated from the parameter name by a colon. All characters after the colon,
 * including any whitespace is considered part of the regex pattern.
 * If no regex is given then the default "[^/]+?" is used.
 * Like a normal URI components are separated with forward slash "/".
 * A parameterized component cannot be mixed with a string component. e.g. the following is invalid
 * /test/abc{param}/123 but the following is valid
 * /test/{param:abc}/123
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class ResourcePath {
    private final String uri;
    private Component[] components;
    private Map<String, Component> namedComponents = new HashMap<>();

    public ResourcePath(final String uri) {
        this.uri = uri;
        parsePath();
    }

    public String getUri() {
        return uri;
    }

    public Component[] getComponents() {
        return components;
    }

    private void parsePath() {
        String[] parts = uri.split("/");
        components = new Component[parts.length];
        int index = 0;
        for (String part : parts) {
            if (part.isEmpty()) {
                //shrink array by 1
                components = Arrays.copyOf(components, components.length - 1, Component[].class);
                continue;
            }
            Component component = new Component();
            component.setComponentValue(part);
            if (part.startsWith("{") && part.endsWith("}")) {
                int colonIndex = part.indexOf(':');
                if (colonIndex != -1) {
                    //named parameter (minus the {})
                    String name = part.substring(1, colonIndex);
                    //pattern is everything from : to } (exclusive of both)
                    String pattern = part.substring(colonIndex + 1, part.length() - 1);
                    component.setName(name);
                    component.setPattern(pattern);
                } else {
                    //no colon the whole thing is a parameter name (minus the {})
                    String name = part.substring(1, part.length() - 1);
                    component.setName(name);
                    component.setPattern("[^/]+?");
                }
                namedComponents.put(component.getName(), component);
            }
            components[index] = component;
            index++;
        }
    }

    /**
     * Checks if the given path matches this resource path.
     * A string path matches if the following conditions hold true.
     * <pre>
     *     1) The string path, when split into its components has the same length as
     *          {@link #getComponents()}.length
     *     2) If this resource path contains dynamic components (regex),
     *          each regex must match the component(at the same index) in the string path
     * </pre>
     * NOTE: if a query string is included in the string path given it will be stripped and not
     * included in the comparison. e.g. /home/me/edit?id=123 will become  /home/me/edit
     *
     * @param path the name/path/url to match against
     * @return
     */
    public boolean matches(String path) {
        path = path.trim();
        //since we split on / a root path would create an empty component array
        // so do a manual check against the raw string uri that created this path
        if (path.equalsIgnoreCase("/") && uri.equalsIgnoreCase("/")) {
            return true;
        }
        int qIndex = path.indexOf('?');
        if (qIndex != -1) {
            //remove query string from path
            path = path.substring(0, qIndex);
        }
        String[] parts = path.split("/");
        //use size to avoid re-sizing array on empty components and instead padding with null
        //in these cases parts.length is unreliable for comparison
        int size = parts.length;
        //first make sure empty components are removed, in cases where a URL contains something like some//path/eg
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i].trim();
            if (part.isEmpty()) {
                //set to null (can skip later if null) and -1 from size
                parts[i] = null;
                --size;
            }
        }
        if (components.length != size) {
            return false;
        }
        //iterate over the parts, as soon as the first component doesn't match return false
        int i = 0;
        for (int j = 0; j < parts.length; j++) {
            //skip, we set it to null above
            if (parts[j] == null) {
                continue;
            }
            String component = parts[j];
            Component pathComponent = components[i];
            if (!pathComponent.isPattern()) {
                //if its not a pattern the strings must be equal
                if (!component.equalsIgnoreCase(pathComponent.getComponentValue())) {
                    return false;
                }
            } else {
                //if its a pattern the string must match
                if (!pathComponent.matches(component)) {
                    return false;
                }
            }
            //if we get here then the path component matched the component value
            //so the path's runtime component value should be the extracted component
            pathComponent.setRuntimeValue(component);
            i++;
        }
        //if we get here all the components matched
        return true;
    }

    /**
     * Gets a named component from the resource's path or null if not found
     *
     * @param name the name of the component to return
     * @return
     */
    public Component getComponent(final String name) {
        for (Component component : components) {
            if (component != null && component.isNamed() && name.equals(component.getName())) {
                return component;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        for (Component c : components) {
            b.append(c.getComponentValue()).append("/");
        }
        return "ResourcePath{" +
                "uri='" + b.toString() + '\'' +
                '}';
    }

    public static class Component {
        private String componentValue;
        private boolean isPattern;
        private String name;
        private String runtimeValue;
        private Pattern pattern;

        /**
         * Check if this component is named or not
         *
         * @return
         */
        public boolean isNamed() {
            return name != null;
        }

        /**
         * Get the value that was parsed  out of the uri comparison which resulted in this instance
         *
         * @return
         */
        public String getRuntimeValue() {
            return runtimeValue;
        }

        /**
         * Set the runtime component value of a path component. This value goes with {@link #getName()} to
         * make a key value pair. Wherever {@link #getName()} is used as a parameter the value set with
         * this method will be injected in its place
         *
         * @param runtimeValue
         */
        public void setRuntimeValue(final String runtimeValue) {
            this.runtimeValue = runtimeValue;
        }

        public String getComponentValue() {
            return componentValue;
        }

        public void setComponentValue(final String value) {
            this.componentValue = value;
        }

        public boolean isPattern() {
            return isPattern;
        }

        public void setPattern(final boolean pattern) {
            isPattern = pattern;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public void setPattern(final String pattern) {
            this.pattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            setPattern(true);
        }

        public Pattern getPattern() {
            return pattern;
        }

        /**
         * @param component the string component to compare this path component to
         * @return true if the path's patter matches the given string, false otherwise
         */
        public boolean matches(final String component) {
            Matcher matcher = pattern.matcher(component);
            return matcher.matches();
        }
    }
}
