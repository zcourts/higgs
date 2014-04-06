package io.higgs.core.files;

/**
 * Represents a file from the underlying file system.
 * The source of the file can be the disk file system directly or from within a JAR file.
 * A resolved file will have information about where the file was found (disk/jar)
 * and a stream from which it's data can be read.
 * if the underlying file is a directory this does not apply
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class ResolvedFile {
}
