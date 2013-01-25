package com.fillta.higgs.http.server.files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Parent ClassLoader passed to this constructor
 * will be used if this ClassLoader can not resolve a
 * particular class.
 * <p/>
 * see http://www.javablogging.com/java-classloader-2-write-your-own-classloader/
 * see http://kalanir.blogspot.co.uk/2010/01/how-to-write-custom-class-loader-to.html
 * see http://tutorials.jenkov.com/java-reflection/dynamic-class-loading-reloading.html
 * see http://javolution.org/
 * param parent Parent ClassLoader (may be from getClass().getClassLoader())
 * or Thread.currentThread().getContextClassLoader() [preferred]
 */
public class StaticClassLoader extends ClassLoader {
    Logger log = LoggerFactory.getLogger(getClass());
    StaticClassPathCache cache = new StaticClassPathCache(".*.class");
    Map<String, Class<?>> defined = new HashMap<>();

    /**
     * Loads a given class from .class file just like
     * the default ClassLoader. This method could be
     * changed to load the class over network from some
     * other server or from the database.
     *
     * @param name Full class name
     */
    public Class<?> getClass(String name, String file) {
        //never try to load core classes
        if (file.startsWith("java") || file.startsWith("/java")
                || file.startsWith("com/sun") || file.startsWith("/com/sun")
                || file.startsWith("com/oracle") || file.startsWith("/com/oracle")) {
            return null;
        }
        Class<?> klass = defined.get(name);
        if (klass != null) {
            return klass;
        }
        byte[] bytes;
        // This loads the byte code data from the file
        bytes = loadClassData(file);
        if (bytes != null) {
            try {
                // defineClass is inherited from the ClassLoader class
                // and converts the byte array into a Class
                Class<?> c = defineClass(name, bytes, 0, bytes.length);
                resolveClass(c);
                return c;
            } catch (Throwable t) {
                throw new RuntimeException("", t);
            }
        }
        return null;
    }

    /**
     * Load a class with the given name.
     *
     * @param name Full class name
     */
    public Class<?> loadClass(String name) {
        //File.separatorChar is OS dependent and cache always stores urls with / separator
        String file = name.replace('.', '/') + ".class";
        Class<?> klass = null;
        RuntimeException ex = null;
        try {
            //try loading with parent first
            klass = super.loadClass(name);
        } catch (Throwable e) {
            ex = new RuntimeException("Class not found", e);
            //not found, try searching classpath cache
            klass = getClass(name, file);
        }
        if (klass != null) {
            //class defined, keep hold of it, save look up next time
            defined.put(name, klass);
        } else {
            //class not found by parent or in cache
            if (ex != null) {
                throw ex;
            } else {
                throw new RuntimeException(
                        "Class not found"
                        , new ClassNotFoundException(String.format("Unable to find class %s as file %s", name, file))
                );
            }
        }
        return klass;
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
    private byte[] loadClassData(String name) {
        // Opening the file
        InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(name);
        if (stream != null) {
            try {
                int size = stream.available();
                byte[] buff = new byte[size];
                DataInputStream in = new DataInputStream(stream);
                // Reading the binary data
                in.readFully(buff);
                in.close();
                return buff;
            } catch (IOException e) {
                return null;
            }
        } else {
            if (cache.contains(name)) {
                return cache.load(name);
            } else {
                return null;
            }
        }
    }
}
