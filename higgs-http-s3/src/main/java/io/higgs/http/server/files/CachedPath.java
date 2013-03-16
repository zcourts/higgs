package io.higgs.http.server.files;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
class CachedPath {
    private String filename, path;
    private boolean isJar;

    CachedPath(final String filename, final String path, final boolean jar) {
        this.filename = filename;
        this.path = path;
        isJar = jar;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(final String filename) {
        this.filename = filename;
    }

    public String getPath() {
        return path;
    }

    public void setPath(final String path) {
        this.path = path;
    }

    public boolean isJar() {
        return isJar;
    }

    public void setJar(final boolean jar) {
        isJar = jar;
    }

    @Override
    public String toString() {
        return "CachedPath{" +
                "filename='" + filename + '\'' +
                ", path='" + path + '\'' +
                ", isJar=" + isJar +
                '}';
    }
}
