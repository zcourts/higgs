package info.crlog.higgs.protocols.http

import java.io.File
import java.nio.file.Files

/**
 * Create a reference to a file to be uploaded and configure its associated properties
 * Only difference between this and the {@link HttpFile} class is this does not have a name.
 * It is meant to be used in cases where 1 name is used for multiple {@link PartialHttpFile}s
 * @param file the file to be uploaded (if not Multipart mode, only the filename will be included)
 * @param isText True if this file should be transmitted in Text format (else binary)
 * @author Courtney Robinson <courtney@crlog.info>
 */
case class PartialHttpFile(file: File,
                           isText: Boolean = false) {
  /**
   * contentType the associated contentType for the File
   */
  var contentType: String = Files.probeContentType(file.toPath())

}
