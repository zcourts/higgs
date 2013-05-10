package io.higgs.http.server.transformers;

import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class JarFile {

    /**
     * The ZIP/JAR file
     */
    ZipFile zip;

    /**
     * The file that this instance represents
     */
    ZipEntry entry;

    /**
     * An input stream opened to the file
     */
    InputStream inputStream;

    public JarFile(ZipFile zip, ZipEntry entry, InputStream inputStream) {
        this.zip = zip;
        this.entry = entry;
        this.inputStream = inputStream;
    }

    public ZipFile getZip() {
        return zip;
    }

    public ZipEntry getEntry() {
        return entry;
    }

    public InputStream getInputStream() {
        return inputStream;
    }
}
