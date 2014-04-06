package io.higgs.core;

import io.higgs.core.files.ResolvedFile;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class FileUtil {
    protected FileUtil() {
    }

    /**
     * Given a path, try locating the file/directory represented by it.
     * First check the file system, then check within the current JAR.
     * In both cases the path must be a child of the base directory provided.
     * @param path
     * @return A resolved file or raise a file not found exception
     */
    public static ResolvedFile resolve(String path) {
        return null;
    }
}
