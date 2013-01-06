package com.fillta.higgs.http.server.files;

import com.fillta.higgs.http.server.Endpoint;
import com.fillta.higgs.http.server.HttpRequest;
import com.fillta.higgs.http.server.HttpServer;
import com.fillta.higgs.http.server.ResourceFilter;
import io.netty.handler.codec.http.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

import static io.netty.handler.codec.http.HttpHeaders.Names.IF_MODIFIED_SINCE;

/**
 * todo this class and the rest of this package needs refactoring and cleaning up like there's no tomorrow!
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class StaticResourceFilter implements ResourceFilter {
	public static final String HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";
	public static final String HTTP_DATE_GMT_TIMEZONE = "GMT";
	public static final int HTTP_CACHE_SECONDS = 60;
	private HttpServer server;
	private Logger log = LoggerFactory.getLogger(getClass());

	public StaticResourceFilter(final HttpServer server) {
		this.server = server;
	}

	@Override
	public Endpoint getEndpoint(final HttpRequest request) {
		if (!request.getMethod().getName().equalsIgnoreCase(HttpMethod.GET.getName()))
			return null;
		String base_dir = server.getConfig().files.public_directory;
		String uri = request.getUri();
		//remove query string from path
		if (uri.indexOf("?") != -1) {
			uri = uri.substring(0, uri.indexOf("?"));
		}
		if (uri.equals("/") && server.getConfig().files.serve_index_file) {
			uri = server.getConfig().files.index_file;
		}
		//the URL must be from the public directory
		if (!uri.startsWith("/"))
			uri = "/" + uri;
		if (base_dir.endsWith("/"))
			uri = uri.substring(1);
		//sanitize before use
		uri = base_dir + sanitizeUri(uri);
		File file = null;
		//check the classpath first
		URL source = Thread.currentThread().getContextClassLoader().getResource(uri);
		try {
			if (source != null) {
				//jar:file:/B:/dev/projects/Higgs/higgs-http-s3/target/higgs-http-3s-0.0.1-SNAPSHOT.jar!/public/default.html
				String url = source.toExternalForm();
				if (url.startsWith("jar:")) {
					//if it's a JAR path see if we can get it
					Endpoint e = returnJarStreamEndpoint(url);
					if (e != null) {
						return e;
					}
					//if we couldn't get it from the JAR continue anyway and see if it exists on disk
				} else {
					file = new File(source.toURI());
					if (file.isHidden() || !file.exists())
						file = null;
				}
			}
		} catch (Throwable e) {
		}
		//if we couldn't load it from the class path then try to get it from disk
		if (file == null) {
			File base = new File(base_dir);
			if (!base.exists()) {
				log.warn("Public files directory that is configured does not exist. Will not serve static files");
				return null;
			}
			file = new File(uri);
			if (file.isHidden() || !file.exists()) {
				return null;
			}
			//if its not a sub directory tell them no!
			if (!isSubDirectory(base, file))
				return null;
		}
		if (file.isDirectory()) {
			//get list of files, if index/default found then send it instead of listing directory
			try {
				DirectoryStream<Path> paths = Files.newDirectoryStream(file.toPath());
				boolean list = true;
				for (Path path : paths) {
					Path name = path.getFileName();
					if (server.getConfig().files.index_file.endsWith(name.toString())) {
						file = path.toFile();
						list = false;
						break;
					}
				}
				if (list) {
					if (server.getConfig().files.enable_directory_listing) {
						return new Endpoint("" + System.nanoTime(), server, StaticFile.class, StaticFile.DIRECTORY,
								StaticFile.SERVER_FILE_CONSTRUCTOR, server, file);
					} else {
						return null;//directory listing not enabled return 404 or another error
					}
				}
			} catch (IOException e) {
				log.info(String.format("Failed to list files in directory {%s}", e.getMessage()));
				return null;
			}
		}
		if (!file.isFile())
			return null;
		// Cache Validation
		String ifModifiedSince = request.getHeader(IF_MODIFIED_SINCE);
		//disable modified since - currently broken
		if (false == true && ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
			SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
			Date ifModifiedSinceDate = null;
			try {
				ifModifiedSinceDate = dateFormatter.parse(ifModifiedSince);
				// Only compare up to the second because the datetime format we send to the client
				// does not have milliseconds
				long ifModifiedSinceDateSeconds = ifModifiedSinceDate.getTime() / 1000;
				long fileLastModifiedSeconds = file.lastModified() / 1000;
				if (ifModifiedSinceDateSeconds == fileLastModifiedSeconds) {
					return new Endpoint("" + System.nanoTime(), server, StaticFile.class, StaticFile.NOT_MODIFIED,
							StaticFile.SERVER_FILE_CONSTRUCTOR, server, file);
				}
			} catch (ParseException e) {
				log.warn("Unable to parse modified since date, ignoring and sending file...");
			}
		}
		return new Endpoint("" + System.nanoTime(), server, StaticFile.class, StaticFile.SEND_FILE,
				StaticFile.SERVER_FILE_CONSTRUCTOR, server, file);
	}

	private Endpoint returnJarStreamEndpoint(final String url) throws IOException {
		return null;
		//use classpath similar to  this to test:
		//java  -Xdebug -X"runjdwp:transport=dt_socket,server=y,suspend=n,address=5005" -classpath "B:\Courtney\.m2\repository\org\slf4j\slf4j-api\1.6.1\slf4j-api-1.6.1.jar;B:\Courtney\.m2\repository\org\slf4j\slf4j-log4j12\1.7.0\slf4j-log4j12-1.7.0.jar;B:\Courtney\.m2\repository\log4j\log4j\1.2.17\log4j-1.2.17.jar;B:\Courtney\.m2\repository\com\google\guava\guava\13.0.1\guava-13.0.1.jar;B:\Courtney\.m2\repository\io\netty\netty\4.0.0.Beta1-SNAPSHOT\netty-4.0.0.Beta1-SNAPSHOT.jar;B:\Courtney\.m2\repository\io\netty\netty-metrics-yammer\4.0.0.Beta1-SNAPSHOT\netty-metrics-yammer-4.0.0.Beta1-SNAPSHOT.jar;B:\Courtney\.m2\repository\io\netty\netty-common\4.0.0.Beta1-SNAPSHOT\netty-common-4.0.0.Beta1-SNAPSHOT.jar;B:\Courtney\.m2\repository\com\yammer\metrics\metrics-core\2.1.4\metrics-core-2.1.4.jar;B:\Courtney\.m2\repository\com\fasterxml\jackson\core\jackson-databind\2.1.2\jackson-databind-2.1.2.jar;B:\Courtney\.m2\repository\com\fasterxml\jackson\core\jackson-annotations\2.1.1\jackson-annotations-2.1.1.jar;B:\Courtney\.m2\repository\com\fasterxml\jackson\core\jackson-core\2.1.1\jackson-core-2.1.1.jar;B:\Courtney\.m2\repository\org\thymeleaf\thymeleaf\2.0.15\thymeleaf-2.0.15.jar;B:\Courtney\.m2\repository\ognl\ognl\3.0.5\ognl-3.0.5.jar;B:\Courtney\.m2\repository\org\javassist\javassist\3.16.1-GA\javassist-3.16.1-GA.jar;B:\Courtney\.m2\repository\org\yaml\snakeyaml\1.11\snakeyaml-1.11.jar;B:\dev\projects\Higgs\higgs-core\target\higgs-core-0.0.1-SNAPSHOT.jar;B:\dev\projects\Higgs\higgs-http-s3\target\higgs-http-3s-0.0.1-SNAPSHOT.jar;B:\dev\projects\Higgs\higgs-http-s3\src\main\resources" com.fillta.higgs.http.server.demo.HttpServerDemo
		//todo support  serving files from JARs
		//need to implement or extend StaticFile so thatit accepts an input stream.
		//then modify sendFile method to send a netty ChunkedStream passing in the jar input stream
//		String jar = url.substring(url.indexOf(":") + 1, url.indexOf("!"));
//		String jarFile = url.substring(url.indexOf("!") + 1);
//		ZipFile zip = new ZipFile(jar);
//		if (zip != null) {
//			Enumeration<? extends ZipEntry> entries = zip.entries();
//			if (entries != null) {
//				while (entries.hasMoreElements()) {
//					ZipEntry entry = entries.nextElement();
//					if (jarFile.equalsIgnoreCase(entry.getName())) {
////									zip.getInputStream()
//					}
//				}
//			}
//		}
	}

	/**
	 * Checks, whether the child directory is a subdirectory of the base
	 * directory.
	 * http://www.java2s.com/Tutorial/Java/0180__File/Checkswhetherthechilddirectoryisasubdirectoryofthebasedirectory.htm
	 *
	 * @param base  the base directory.
	 * @param child the suspected child directory.
	 * @return true, if the child is a subdirectory of the base directory.
	 * @throws IOException if an IOError occured during the test.
	 */
	public boolean isSubDirectory(File base, File child) {
		try {
			base = base.getCanonicalFile();
			child = child.getCanonicalFile();
			File parentFile = child;
			while (parentFile != null) {
				if (base.equals(parentFile)) {
					return true;
				}
				parentFile = parentFile.getParentFile();
			}
		} catch (IOException e) {

		}
		return false;
	}

	private static final Pattern INSECURE_URI = Pattern.compile(".*[<>&\"].*");

	private String sanitizeUri(String uri) {
		// Decode the path.
		try {
			uri = URLDecoder.decode(uri, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			try {
				uri = URLDecoder.decode(uri, "ISO-8859-1");
			} catch (UnsupportedEncodingException e1) {
				throw new Error();
			}
		}

		if (!uri.startsWith("/")) {
			return null;
		}

		// Convert file separators.
		uri = uri.replace('/', File.separatorChar);
		// TODO provide more secure checks
		if (uri.contains(File.separator + '.') ||
				uri.contains('.' + File.separator) ||
				uri.startsWith(".") || uri.endsWith(".") ||
				INSECURE_URI.matcher(uri).matches()) {
			return null;
		}

		return uri;
	}
}
