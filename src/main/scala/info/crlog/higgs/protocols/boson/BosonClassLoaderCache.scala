package info.crlog.higgs.protocols.boson

import java.io.{FileInputStream, DataInputStream, File, IOException}
import java.util.Enumeration
import java.util.regex.Pattern
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import collection.mutable.ListBuffer
import collection.mutable.Map

/**
 * We keep a cache class name -> meta to
 * save having to search the file system at run time.
 * We're using a cache and don't load all the classes because some
 * class paths can be really messy, lots and lots of jars that could result in
 * anything from a few MB to hundreds.
 * Essentially "lazy" loading the same way the JVM does but a tad better.
 * @see http://docs.oracle.com/javase/specs/
 * @see http://stackoverflow.com/questions/5702423/does-jvm-loads-all-used-classes-when-loading-a-particular-class
 * @see http://javolution.org/
 * @see http://tutorials.jenkov.com/java-reflection/dynamic-class-loading-reloading.html
 * @see http://docs.oracle.com/javase/7/docs/technotes/guides/lang/cl-mt.html
 * @see http://www.javablogging.com/java-classloader-2-write-your-own-classloader/
 * @see http://kalanir.blogspot.co.uk/2010/01/how-to-write-custom-class-loader-to.html
 * @see http://stackoverflow.com/questions/3923129/get-a-list-of-resources-from-classpath-directory
 * @see http://stackoverflow.com/a/3923182/400048
 *      list resources available from the classpath @ *
 */
class BosonClassLoaderCache {
  //only .class files are cached
  val cache = getResources(Pattern.compile(".*.class"))

  /**
   *
   * @param klass  the full path to the class including extension e.g.
   *               com/domain/product/MyClass.class
   * @return   true if the file is on the class path
   */
  def contains(klass: String): Boolean = cache.contains(klass)

  def load(klass: String): Array[Byte] = {
    cache get klass match {
      case None => null
      case Some(el) => {
        val file = new File(el.path)
        if (el.isJar) {
          val jar = new ZipFile(file)
          val entry = jar.getEntry(klass)
          val stream = jar.getInputStream(entry)
          val size = stream.available
          val buff = new Array[Byte](size)
          val in = new DataInputStream(stream)
          // Reading the binary data
          in.readFully(buff)
          in.close
          return buff
        } else {
          val stream = new DataInputStream(new FileInputStream(file))
          val size = stream.available
          val buff = new Array[Byte](size)
          val in = new DataInputStream(stream)
          // Reading the binary data
          in.readFully(buff)
          in.close
          return buff
        }
      }
    }
  }

  /**
   * for all elements of java.class.path get a Collection of resources Pattern
   * pattern = Pattern.compile(".*"); gets all resources
   *
   * @param pattern the pattern to match
   * @return the resources in the order they are found
   */
  def getResources(pattern: Pattern): Map[String, BosonClassLoaderElement] = {
    val retval = Map.empty[String, BosonClassLoaderElement]
    val classPath: String = System.getProperty("java.class.path", "")
    val classPathElements: Array[String] = classPath.split(System.getProperty("path.separator"))
    for (element <- classPathElements) {
      retval ++= getResources(element, pattern)
    }
    retval
  }

  private def getResources(element: String, pattern: Pattern): Map[String, BosonClassLoaderElement] = {
    val retval = Map.empty[String, BosonClassLoaderElement]
    val file: File = new File(element)
    if (file.isDirectory) {
      val rc = getResourcesFromDirectory(file, pattern)
      for (resource <- rc) {
        retval += resource -> new BosonClassLoaderElement(resource, element, false)
      }
    }
    else {
      if (file.isFile && file.exists) {
        val rc = getResourcesFromJarFile(file, pattern)
        for (resource <- rc) {
          retval += resource -> new BosonClassLoaderElement(resource, element, true)
        }
      }
    }
    retval
  }

  private def getResourcesFromJarFile(file: File, pattern: Pattern): ListBuffer[String] = {
    val retval = ListBuffer.empty[String]
    try {
      val zf: ZipFile = new ZipFile(file)
      val e: Enumeration[_] = zf.entries
      while (e.hasMoreElements) {
        val ze: ZipEntry = e.nextElement.asInstanceOf[ZipEntry]
        val fileName: String = ze.getName
        val accept: Boolean = pattern.matcher(fileName).matches
        if (accept) {
          retval += fileName
        }
      }
      try {
        zf.close
      } catch {
        case e1: IOException => {
        }
      }
      return retval
    }
    catch {
      case e => {
        e.printStackTrace
      }
    }
    return null
  }

  private def getResourcesFromDirectory(directory: File, pattern: Pattern): ListBuffer[String] = {
    val retval = ListBuffer.empty[String]
    val fileList: Array[File] = directory.listFiles
    for (file <- fileList) {
      if (file.isDirectory) {
        retval ++= getResourcesFromDirectory(file, pattern)
      } else {
        try {
          val fileName: String = file.getCanonicalPath
          val accept: Boolean = pattern.matcher(fileName).matches
          if (accept) {
            retval += fileName
          }
        } catch {
          case e: IOException => {
            throw new Error(e)
          }
        }
      }
    }
    return retval
  }
}