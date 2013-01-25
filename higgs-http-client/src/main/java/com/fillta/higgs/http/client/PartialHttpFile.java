package com.fillta.higgs.http.client;

import java.io.File;

/**
 * Create a reference to a file to be uploaded and configure its associated properties
 * Only difference between this and the {@link HttpFile} class is this does not have a name.
 * It is meant to be used in cases where 1 name is used for multiple {@link PartialHttpFile}s
 *
 * @param file   the file to be uploaded (if not Multipart mode, only the filename will be included)
 * @param isText True if this file should be transmitted in Text format (else binary)
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class PartialHttpFile extends HttpFile {
    private final String name;

    public PartialHttpFile(File file) {
        this(file, false);
    }

    public PartialHttpFile(File file, boolean isText) {
        this(file, null, isText);
        contentType(file.getName(), file);
    }

    public PartialHttpFile(File file, String contentType, boolean isText) {
        super(null, file, contentType, isText);
        name = null;
    }
}
