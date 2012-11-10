package info.crlog.higgs.protocols.jrpc.reflect

import java.net.{URLDecoder, URL}
import java.io.File
import collection.mutable.ListBuffer
import java.util.jar.JarFile

/**
 * Ported from http://dzone.com/snippets/get-all-classes-within-package
 * @author Courtney Robinson <courtney@crlog.info>
 */
object PackageScanner {
  def get(packagePath: String): ListBuffer[Class[_]] = {
    val classLoader: ClassLoader = Thread.currentThread.getContextClassLoader
    var packageURL: URL = null
    val files = ListBuffer.empty[Class[_]]
    val packageName = packagePath.replace(".", "/")
    packageURL = classLoader.getResource(packageName)
    if (packageURL.getProtocol == "jar") {
      var jarFileName: String = null
      var jf: JarFile = null
      var entryName: String = null
      jarFileName = URLDecoder.decode(packageURL.getFile, "UTF-8")
      jarFileName = jarFileName.substring(5, jarFileName.indexOf("!"))
      System.out.println(">" + jarFileName)
      jf = new JarFile(jarFileName)
      val jarEntries = jf.entries
      while (jarEntries.hasMoreElements) {
        entryName = jarEntries.nextElement.getName
        if (entryName.startsWith(packageName) && entryName.length > packageName.length + 5) {
          entryName = entryName.substring(packageName.length, entryName.lastIndexOf('.'))
          entryName = entryName.replace('/', '.').replace('\\', '.')
          if (!entryName.startsWith(".")) {
            entryName = "." + entryName
          }
          files += Class.forName(packagePath + entryName)
        }
      }
    } else {
      val folder: File = new File(packageURL.getFile)
      val contenuti: Array[File] = folder.listFiles
      var entryName: String = null
      for (actual <- contenuti) {
        entryName = actual.getName
        if (entryName.contains(".")) {
          entryName = entryName.substring(0, entryName.lastIndexOf('.'))
          files += Class.forName(packageName.replace("/", ".") + "." + entryName)
        }
      }
    }
    return files
  }

  /**
   * Scans all classes accessible from the context class loader which belong to the given package and subpackages.
   *
   * @param packageName The base package
   * @return The classes
   * @throws ClassNotFoundException
   * @throws IOException
   */
  def getOld(packageName: String): ListBuffer[Class[_]] = {
    //    val src: CodeSource = getClass.getProtectionDomain().getCodeSource()
    //    if (src != null) {
    //      val jar: URL = src.getLocation()
    //      val zip: ZipInputStream = new ZipInputStream(jar.openStream())
    //      /* Now examine the ZIP file entries to find those you care about. */
    //      var entry=zip.getNextEntry
    //      while(entry!=null){
    //        getClass.getResource(entry.getName)
    //        entry=zip.getNextEntry
    //      }
    //    }
    //    else {
    //      /* Fail... */
    //    }
    val classLoader: ClassLoader = Thread.currentThread.getContextClassLoader
    val classes = ListBuffer.empty[Class[_]]
    if (classLoader != null) {
      val path: String = packageName.replace('.', '/')
      val resources = classLoader.getResources(path)
      val dirs = ListBuffer.empty[File]
      while (resources.hasMoreElements) {
        val r: URL = resources.nextElement
        dirs += new File(r.getPath)
      }
      for (directory <- dirs) {
        classes ++= findClasses(directory, packageName)
      }
    }
    return classes
  }

  /**
   * Recursive method used to find all classes in a given directory and subdirs.
   *
   * @param directory   The base directory
   * @param packageName The package name for classes found inside the base directory
   * @return The classes
   * @throws ClassNotFoundException
   */
  private def findClasses(directory: File, packageName: String): ListBuffer[Class[_]] = {
    val classes = ListBuffer.empty[Class[_]]
    if (!directory.exists) {
      return classes
    }
    val files = directory.listFiles
    for (file <- files) {
      if (file.isDirectory) {
        assert(!file.getName.contains("."))
        classes ++= findClasses(file, packageName + "." + file.getName)
      }
      else if (file.getName.endsWith(".class")) {
        classes += Class.forName(packageName + '.' + file.getName.substring(0, file.getName.length - 6))
      }
    }
    return classes
  }
}
