import java.io.File;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class Boom {
    public static ArrayList<Class> getClassNamesFromPackage(String packageName) throws Exception {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL packageURL;
        ArrayList<Class> files = new ArrayList<Class>();
        packageName = packageName.replace(".", "/");
        packageURL = classLoader.getResource(packageName);
        if (packageURL.getProtocol().equals("jar")) {
            String jarFileName;
            JarFile jf;
            Enumeration<JarEntry> jarEntries;
            String entryName;
            // build jar file name, then loop through zipped entries
            jarFileName = URLDecoder.decode(packageURL.getFile(), "UTF-8");
            jarFileName = jarFileName.substring(5, jarFileName.indexOf("!"));
            System.out.println(">" + jarFileName);
            jf = new JarFile(jarFileName);
            jarEntries = jf.entries();
            while (jarEntries.hasMoreElements()) {
                entryName = jarEntries.nextElement().getName();
                if (entryName.startsWith(packageName) && entryName.length() > packageName.length() + 5) {
                    entryName = entryName.substring(packageName.length(), entryName.lastIndexOf('.'));
                    files.add(Class.forName(packageName.replace("/", ".") + "." + entryName));
                }
            }
            // loop through files in classpath
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
        return files;
    }

    public static void main(String[] args) throws Exception {
        for (Class i : getClassNamesFromPackage("info.crlog.higgs")) {
            System.out.println(i.getName());
        }
    }
}
