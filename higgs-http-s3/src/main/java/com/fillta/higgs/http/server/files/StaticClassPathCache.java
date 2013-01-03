package com.fillta.higgs.http.server.files;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Originally written to load only .class files from the class path but also used to load other resources
 * now
 * We keep a cache class name -> meta to
 * save having to search the file system at run time.
 * We're using a cache and don't load all the classes because some
 * class paths can be really messy, lots and lots of jars that could result in
 * anything from a few MB to hundreds.
 * Essentially "lazy" loading the same way the JVM does but a tad better for our use cases.
 * <p/>
 * see http://docs.oracle.com/javase/specs/
 * see http://stackoverflow.com/questions/5702423/does-jvm-loads-all-used-classes-when-loading-a-particular-class
 * see http://javolution.org/
 * see http://tutorials.jenkov.com/java-reflection/dynamic-class-loading-reloading.html
 * see http://docs.oracle.com/javase/7/docs/technotes/guides/lang/cl-mt.html
 * see http://www.javablogging.com/java-classloader-2-write-your-own-classloader/
 * see http://kalanir.blogspot.co.uk/2010/01/how-to-write-custom-class-loader-to.html
 * see http://stackoverflow.com/questions/3923129/get-a-list-of-resources-from-classpath-directory
 * see http://stackoverflow.com/a/3923182/400048
 * list resources available from the classpath @ *
 */
public class StaticClassPathCache {
	//only .class files are cached
	private final Map<String, CachedPath> cache;

	public StaticClassPathCache() {
		this(".*.class");
	}

	public StaticClassPathCache(String pattern) {
		this.cache = getResources(Pattern.compile(pattern));
	}

	public Map<String, CachedPath> get() {
		return cache;
	}

	/**
	 * @param file the full path to the class including extension e.g.
	 *             com/domain/product/MyClass.class
	 * @return true if the file is on the class path
	 */
	public boolean contains(String file) {
		return cache.containsKey(file);
	}

	public byte[] load(String klass) {
		CachedPath el = cache.get(klass);
		if (el != null) {
			try {
				File file = new File(el.getPath());
				if (el.isJar()) {
					ZipFile jar = new ZipFile(file);
					ZipEntry entry = jar.getEntry(klass);
					InputStream stream = jar.getInputStream(entry);
					int size = stream.available();
					byte[] buff = new byte[size];
					DataInputStream in = new DataInputStream(stream);
					// Reading the binary data
					in.readFully(buff);
					in.close();
					return buff;
				} else {
					DataInputStream stream = new DataInputStream(new FileInputStream(file));
					int size = stream.available();
					byte[] buff = new byte[size];
					DataInputStream in = new DataInputStream(stream);
					// Reading the binary data
					in.readFully(buff);
					in.close();
					return buff;
				}
			} catch (Exception e) {
				//return empty byte array
			}
		}
		return null;
	}

	/**
	 * for all elements of java.class.path get a Collection of resources Pattern
	 * pattern = Pattern.compile(".*"); gets all resources
	 *
	 * @param pattern the pattern to match
	 * @return the resources in the order they are found
	 */
	public Map<String, CachedPath> getResources(Pattern pattern) {
		Map<String, CachedPath> retval = new HashMap<>();
		String classPath = System.getProperty("java.class.path", "");
		String[] classPathElements = classPath.split(System.getProperty("path.separator"));
		for (String element : classPathElements) {
			retval.putAll(getResources(element, pattern));
		}
		return retval;
	}

	public Map<String, CachedPath> getResources(String element, Pattern pattern) {
		Map<String, CachedPath> retval = new HashMap<>();
		File file = new File(element);
		if (file.isDirectory()) {
			List<String> rc = getResourcesFromDirectory(file, pattern);
			for (String resource : rc) {
				retval.put(resource, new CachedPath(resource, element, false));
			}
		} else {
			if (file.isFile() && file.exists()) {
				List<String> rc = getResourcesFromJarFile(file, pattern);
				for (String resource : rc) {
					retval.put(resource, new CachedPath(resource, element, true));
				}
			}
		}
		return retval;
	}

	public List<String> getResourcesFromJarFile(File file, Pattern pattern) {
		List<String> retval = new ArrayList<>();
		try {
			ZipFile zf = new ZipFile(file);
			Enumeration<?> e = zf.entries();
			while (e.hasMoreElements()) {
				ZipEntry ze = (ZipEntry) e.nextElement();
				String fileName = ze.getName();
				boolean accept = pattern.matcher(fileName).matches();
				if (accept) {
					retval.add(fileName);
				}
			}
			zf.close();
			return retval;
		} catch (IOException ioe) {
			//log.warn("IO Exception", ioe);
		}
		return null;
	}

	public List<String> getResourcesFromDirectory(File directory, Pattern pattern) {
		List<String> retval = new ArrayList<>();
		File[] fileList = directory.listFiles();
		for (File file : fileList) {
			if (file.isDirectory()) {
				retval.addAll(getResourcesFromDirectory(file, pattern));
			} else {
				try {
					String fileName = file.getCanonicalPath();
					boolean accept = pattern.matcher(fileName).matches();
					if (accept) {
						retval.add(fileName);
					}
				} catch (IOException ioe) {
					//log.warn("IO Exception", ioe);
				}
			}
		}
		return retval;
	}
}