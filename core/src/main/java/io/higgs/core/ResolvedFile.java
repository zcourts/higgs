package io.higgs.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.ClosedChannelException;
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
        } catch (ClosedChannelException ignored) {
            return knownSize;
        } catch (IOException e) {
            log.warn("Failed to get available bytes on stream ", e);
        }
        return -2;
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
            log.warn("Failed to get last modification time of a file " + path, e);
            return 0;
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
}
