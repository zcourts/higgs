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
import java.net.URLDecoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

import static io.netty.handler.codec.http.HttpHeaders.Names.IF_MODIFIED_SINCE;

/**
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
		String uri = request.getUri();
		if (uri.equals("/") && server.getConfig().files().serve_index_file) {
			uri = server.getConfig().files().index_file;
		}
		//the URL must be from the public directory
		if (!uri.startsWith("/"))
			uri = "/" + uri;
		if (server.getConfig().files().public_directory.endsWith("/"))
			uri = uri.substring(1);
		uri = server.getConfig().files().public_directory + uri;
		final String path = sanitizeUri(uri);
		if (path == null) {
			return null;
		}
		File base = new File(sanitizeUri(server.getConfig().files().public_directory));
		if (!base.exists()) {
			log.warn("Public files directory that is configured does not exist. Will not serve static files");
			return null;
		}
		File file = new File(path);
		if (file.isHidden() || !file.exists()) {
			return null;
		}
		//if its not a sub directory tell them no!
		if (!isSubDirectory(base, file))
			return null;
		if (file.isDirectory()) {
			if (server.getConfig().files().enable_directory_listing)
				return new Endpoint("" + System.nanoTime(), server, StaticFile.class, StaticFile.DIRECTORY,
						StaticFile.SERVER_FILE_CONSTRUCTOR, server, file);
			return null;//directory listing not enabled return 404 or another error
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

	private static String sanitizeUri(String uri) {
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

		// Convert to absolute path.
		return System.getProperty("user.dir") + File.separator + uri;
	}
}
