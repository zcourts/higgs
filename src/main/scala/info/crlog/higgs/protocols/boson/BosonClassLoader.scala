package info.crlog.higgs.protocols.boson

import java.io.{DataInputStream, IOException}
import org.slf4j.LoggerFactory


/**
 * Parent ClassLoader passed to this constructor
 * will be used if this ClassLoader can not resolve a
 * particular class.
 *
 * @see http://www.javablogging.com/java-classloader-2-write-your-own-classloader/
 * @see http://kalanir.blogspot.co.uk/2010/01/how-to-write-custom-class-loader-to.html
 * @see http://tutorials.jenkov.com/java-reflection/dynamic-class-loading-reloading.html
 * @see http://javolution.org/
 * @param parent Parent ClassLoader (may be from getClass().getClassLoader())
 *               or Thread.currentThread().getContextClassLoader() [preferred]
 */
class BosonClassLoader(parent: ClassLoader) extends ClassLoader(parent) {
  val log = LoggerFactory.getLogger(getClass())
  val cache = new BosonClassLoaderCache()
  val defined = collection.mutable.Map.empty[String, Class[_]]

  /**
   * Loads a given class from .class file just like
   * the default ClassLoader. This method could be
   * changed to load the class over network from some
   * other server or from the database.
   *
   * @param name Full class name
   */
  private def getClass(name: String, file: String): Class[_] = {
    //never try to load core classes
    if (file.startsWith("java") || file.startsWith("/java")
      || file.startsWith("com/sun") || file.startsWith("/com/sun")
      || file.startsWith("com/oracle") || file.startsWith("/com/oracle")) {
      return null
    }
    defined get name match {
      case None => {
        var bytes: Array[Byte] = null
        try {
          // This loads the byte code data from the file
          bytes = loadClassData(file)
          if (bytes != null) {
            try {
              // defineClass is inherited from the ClassLoader class
              // and converts the byte array into a Class
              val c = defineClass(name, bytes, 0, bytes.length)
              resolveClass(c)
              return c
            } catch {
              case e => {
                //log.debug("Unable to define or resolve class (%s)" format (name), e)
                return null
              }
            }
          } else {
            return null
          }
        } catch {
          case e: IOException => {
            log.warn("Failed to load class %s " format (name), e)
            return null
          }
        }
      }
      case Some(klass) => return klass
    }
  }

  /**
   * Load a class with the given name.
   *
   * @param name Full class name
   */
  override def loadClass(name: String): Class[_] = {
    //File.separatorChar is OS dependent and cache always stores urls with / separator
    val file = name.replace('.', '/') + ".class"
    var klass: Class[_] = null
    var ex: Throwable = null
    try {
      //try loading with parent first
      klass = super.loadClass(name)
    } catch {
      case e => {
        ex = e
        //not found, try searching classpath cache
        klass = getClass(name, file)
      }
    }
    if (klass != null) {
      //class defined, keep hold of it, save look up next time
      defined += name -> klass
    } else {
      //class not found by parent or in cache
      if (ex != null) {
        throw ex
      } else {
        throw new ClassNotFoundException("Unable to find class %s as file %s" format(name, file))
      }
    }
    klass
  }

  /**
   * Loads a given file (presumably .class) into a byte array.
   * The file should be accessible as a resource, for example
   * it could be located on the classpath.
   *
   * @param name File name to load
   * @return Byte array read from the file
   * @throws IOException Is thrown when there
   *                     was some problem reading the file
   */
  private def loadClassData(name: String): Array[Byte] = {
    // Opening the file
    val stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(name)
    if (stream != null) {
      val size = stream.available
      val buff = new Array[Byte](size)
      val in = new DataInputStream(stream)
      // Reading the binary data
      in.readFully(buff)
      in.close
      return buff
    } else {
      if (cache.contains(name)) {
        return cache.load(name)
      } else {
        return null
      }
    }
  }
}