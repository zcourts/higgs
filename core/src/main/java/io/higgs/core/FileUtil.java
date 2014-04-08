package io.higgs.core;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class FileUtil {
    public static final String PATH_SEPARATOR = System.getProperty("file.separator");

    protected FileUtil() {
    }

    public static ResolvedFile resolve(String baseDir, String file) {
        return resolve(baseDir == null ? null : Paths.get(baseDir), file == null ? null : Paths.get(file));
    }

    public static ResolvedFile resolve(Path file) {
        return resolve(null, file);
    }

    /**
     * Given a path, try locating the file/directory represented by it.
     * First check the file system, then check class path.
     * In both cases the path must be a child of the base directory provided.
     * The file system is checked first because some resources such as configs usually
     * can't be packaged within a JAR because they need to be updated etc...
     *
     * @param baseDir an optional base directory to use. If this is null then the file's path is used directly
     *                otherwise the file's path is resolved against this base directory
     * @param file    the file to get an input stream for
     * @return An input stream to consume data from the file or null if the file cannot be found
     */
    public static ResolvedFile resolve(Path baseDir, Path file) {
        if (file == null) {
            throw new IllegalArgumentException("Path cannot be null or empty");
        }
        ResolvedFile ret = new ResolvedFile();
        if (baseDir != null) {
            file = baseDir.resolve(file);
        }
        ret.setPath(file, baseDir);
        return ret;
    }

    public static ResolvedFile resolve(File file) {
        if (file == null) {
            return null;
        }
        return resolve(file.toPath());
    }

    public static ResolvedFile resolve(Path base, File file) {
        if (file == null) {
            return null;
        }
        return resolve(base, file.toPath());
    }
}
