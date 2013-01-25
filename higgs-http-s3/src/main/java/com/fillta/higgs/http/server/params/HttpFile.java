package com.fillta.higgs.http.server.params;

import io.netty.handler.codec.http.multipart.FileUpload;

import java.io.File;
import java.io.IOException;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class HttpFile {
    private String parameterName;
    private String fileName;
    private String contentType;
    private boolean inMemory;
    private File file;

    public HttpFile(final FileUpload data) {
        parameterName = data.getName();
        fileName = data.getFilename();
        contentType = data.getContentType();
        inMemory = data.isInMemory();
        try {
            file = data.getFile();
        } catch (IOException e) {
            //not possible
        }
    }

    public boolean isInMemory() {
        return inMemory;
    }

    public File getFile() {
        return file;
    }

    /**
     * Get the parameter name used to represent the file in the HTML form
     *
     * @return
     */
    public String getParameterName() {
        return parameterName;
    }

    /**
     * Returns the content type passed by the browser or null if not defined.
     *
     * @return the content type passed by the browser or null if not defined.
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Returns the original filename in the client's filesystem, as provided by the browser
     * (or other client software).
     *
     * @return the original filename
     */
    public String getFileName() {
        return fileName;
    }
}
