package com.fillta.higgs.reflect;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class PackageScanner {
    static Logger log = LoggerFactory.getLogger(PackageScanner.class);

    protected PackageScanner() {
    }

    public static List<Class<?>> get(String packagePath) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL packageURL;
        ArrayList<Class<?>> files = new ArrayList<>();
        try {
            String packageName = packagePath.replace(".", "/");
            packageURL = classLoader.getResource(packageName);
            if (packageURL != null) {
                if (packageURL.getProtocol() == "jar") {
                    String jarFileName = null;
                    JarFile jf = null;
                    String entryName = null;
                    jarFileName = URLDecoder.decode(packageURL.getFile(), "UTF-8");
                    jarFileName = jarFileName.substring(5, jarFileName.indexOf("!"));
                    jf = new JarFile(jarFileName);
                    Enumeration<JarEntry> jarEntries = jf.entries();
                    while (jarEntries.hasMoreElements()) {
                        entryName = jarEntries.nextElement().getName();
                        if (entryName.startsWith(packageName) && entryName.length() > packageName.length() + 5) {
                            entryName = entryName.substring(packageName.length(), entryName.lastIndexOf('.'));
                            entryName = entryName.replace('/', '.').replace('\\', '.');
                            if (!entryName.startsWith(".")) {
                                entryName = "." + entryName;
                            }
                            files.add(Class.forName(packagePath + entryName));
                        }
                    }
                } else {
                    File folder = new File(packageURL.getFile());
                    File[] contenuti = folder.listFiles();
                    String entryName;
                    for (File actual : contenuti) {
                        entryName = actual.getName();
                        if (entryName.contains(".")) {
                            entryName = entryName.substring(0, entryName.lastIndexOf('.'));
                            files.add(Class.forName(packageName.replace("/", ".") + "." + entryName));
                        }
                    }
                }
            }
        } catch (IOException ioe) {
            log.warn("Error scanning package", ioe);
        } catch (ClassNotFoundException e) {
            log.warn("Error scanning package,class not found", e);
        }
        return files;
    }
}
