package info.crlog.higgs.protocols.http

import java.io.File
import java.nio.file.Files

/**
 * Create a reference to a file to be uploaded and configure its associated properties
 *
 * @param name the name of the parameter
 * @param file the file to be uploaded (if not Multipart mode, only the filename will be included)
 * @param contentType the associated contentType for the File
 * @param isText True if this file should be transmitted in Text format (else binary)
 * @author Courtney Robinson <courtney@crlog.info>
 */
case class PartialHttpFile(name: String,
                    file: File,
                    isText: Boolean = false,
                    contentType: String = Files.probeContentType(file.toPath())) {

}
