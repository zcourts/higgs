package io.higgs.core.reflect;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class CachedPath {
    private final String className;
    private String filename, path;
    private boolean isJar;
    private String separator = System.getProperty("file.separator");

    CachedPath(String filename, String path, boolean jar) {
        String className = filename;
        if (className.startsWith(path)) {
            //remove the directory or jar path from the class name
            className = className.substring(path.length());
        }
        if (className.startsWith(separator)) {
            className = className.substring(1);  //replace start / first
        }
        className = className.replace(separator.charAt(0), '.'); //now replace all other slashes

        if (className.endsWith(".class")) {
            className = className.substring(0, className.length() - 6);
        }
        this.className = className;
        this.filename = filename;
        this.path = path;
        isJar = jar;
    }

    /**
     * @return The full path to the file e.g. com/domain/product/MyClass.class
     */
    public String getFilename() {
        return filename;
    }

    /**
     * @return Fully qualified class name e.g. com.domain.product.MyClass
     */
    public String getClassName() {
        return className;
    }

    public void setFilename(final String filename) {
        this.filename = filename;
    }

    /**
     * @return The path to the package or jar containing this file
     */
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
