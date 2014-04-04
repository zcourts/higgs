package io.higgs.http.client;

import javax.activation.MimetypesFileTypeMap;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class HttpFile {
    private List<File> files = new ArrayList<>();
    private final String name;
    private List<String> contentTypes = new ArrayList<>();
    private List<Boolean> texts = new ArrayList<>();
    private MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap();

    public HttpFile(String name, File file) {
        this(name);
        addFile(file);
    }

    public HttpFile(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Cannot create an HttpFile with null as the name");
        }
        this.name = name;
    }

    /**
     * Adds the given file to the set of files under this name. Assumed to be a none text file
     *
     * @param file the file to add
     * @return this
     */
    public HttpFile addFile(File file) {
        return addFile(file, false);
    }

    /**
     * Adds the given file
     *
     * @param file   the file to add
     * @param isText true if the file should be treated as a text file
     * @return this
     */
    public HttpFile addFile(File file, boolean isText) {
        texts.add(texts.size(), isText);
        contentTypes.add(contentTypes.size(), mimeTypesMap.getContentType(file.getPath()));
        files.add(files.size(), file);
        return this;
    }

    public boolean isSingle() {
        return files.size() == 1;
    }

    public String name() {
        return name;
    }

    public File file() {
        return files.get(0);
    }

    public String contentType() {
        return contentTypes.get(0);
    }

    public boolean isText() {
        return texts.get(0);
    }

    public File[] fileSet() {
        return files.toArray(new File[files.size()]);
    }

    public String[] contentTypes() {
        return contentTypes.toArray(new String[contentTypes.size()]);
    }

    public boolean[] isTextSet() {
        boolean[] prim = new boolean[texts.size()];
        for (int i = 0; i < texts.size(); i++) {
            boolean b = texts.get(i);
            prim[i] = b;
        }
        return prim;
    }
}
