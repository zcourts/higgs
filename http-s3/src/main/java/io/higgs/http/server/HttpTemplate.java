package io.higgs.http.server;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class HttpTemplate {
    private String name, path;
    private int code;

    /**
     * @return the name of the template file. e.g. 404.html
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the URI used to match this template e.g. /404-not-found
     *         if this a path is not set then the {@link #getCode()} is returned.
     *         If this is not an error template then {@link #getCode()} is always 0
     */
    public String getPath() {
        return path == null ? String.valueOf(getCode()) : path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getCode() {
        return code;
    }
}
