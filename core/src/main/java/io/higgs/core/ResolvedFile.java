package io.higgs.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class ResolvedFile {
    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected Path path;
    protected InputStream stream;
    protected Path base;
    protected int knownSize;
    protected boolean fromClassPath;
    protected List<Path> dirFiles = new ArrayList<>();
    private long lastModifiedCache;

    public void setPath(Path path) {
        setPath(path, null);
    }

    public void setPath(Path path, Path base) {
        if (path == null) {
            throw new IllegalArgumentException("Path cannot be null");
        }
        this.path = path;
        this.base = base;
        if (exists()) {
            try {
                if (isDirectory()) {
                    DirectoryStream<Path> dir = Files.newDirectoryStream(path);
                    for (Path p : dir) {
                        dirFiles.add(p);
                    }
                } else {
                    stream = Files.newInputStream(path);
                    knownSize = stream.available();
                }
                lastModifiedCache = Files.getLastModifiedTime(path).toMillis();
            } catch (IOException e) {
                log.warn(String.format("Unable to open file %s for reading", path), e);
            }
        } else {
            stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(path.toString());
            fromClassPath = true;
            if (stream != null) {
                try {
                    knownSize = stream.available();
                } catch (IOException ignored) {
                    knownSize = 0;
                }
            }
        }
    }

    /**
     * Gets the size of the underlying path, if the path is a file
     * if the path is a directory -1 is returned, if an error occurs while
     * attempting to get the file size -2 is returned
     *
     * @return the size of the underlying file
     */
    public int size() {
        try {
            return hasStream() ? stream.available() : -1;
        } catch (IOException e) {
            return knownSize;
        }
    }

    public InputStream getStream() {
        return stream;
    }

    public List<Path> getDirectoryIterator() {
        return dirFiles;
    }

    public boolean hasStream() {
        return stream != null;
    }

    /**
     * Tests whether a file is a directory.
     *
     * @return {@code true} if the file is a directory; {@code false} if
     * the file does not exist, is not a directory, or it cannot
     * be determined if the file is a directory or not.
     */
    public boolean isDirectory() {
        return Files.isDirectory(path);
    }

    public boolean exists() {
        return Files.exists(path) || (isFromClassPath() && hasStream());
    }

    public Path getPath() {
        return path;
    }

    public String getName() {
        return path.getFileName().toString();
    }

    public long lastModified() {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            return lastModifiedCache;
        }
    }

    public boolean hasBase() {
        return base != null;
    }

    public Path getBase() {
        return base;
    }

    public boolean isFromClassPath() {
        return fromClassPath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ResolvedFile file = (ResolvedFile) o;

        if (fromClassPath != file.fromClassPath) return false;
        if (knownSize != file.knownSize) return false;
        if (base != null ? !base.equals(file.base) : file.base != null) return false;
        if (dirFiles != null ? !dirFiles.equals(file.dirFiles) : file.dirFiles != null) return false;
        if (path != null ? !path.equals(file.path) : file.path != null) return false;
        if (stream != null ? !stream.equals(file.stream) : file.stream != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = path != null ? path.hashCode() : 0;
        result = 31 * result + (stream != null ? stream.hashCode() : 0);
        result = 31 * result + (base != null ? base.hashCode() : 0);
        result = 31 * result + knownSize;
        result = 31 * result + (fromClassPath ? 1 : 0);
        result = 31 * result + (dirFiles != null ? dirFiles.hashCode() : 0);
        return result;
    }
}
