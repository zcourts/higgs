package com.fillta.higgs.http.server.files;

import com.fillta.functional.Function;
import com.fillta.higgs.events.ChannelMessage;
import com.fillta.higgs.http.server.HttpRequest;
import com.fillta.higgs.http.server.HttpResponse;
import com.fillta.higgs.http.server.HttpServer;
import com.fillta.higgs.http.server.HttpStatus;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.stream.ChunkedFile;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.MimetypesFileTypeMap;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import static io.netty.handler.codec.http.HttpHeaders.Names.*;
import static io.netty.handler.codec.http.HttpHeaders.setContentLength;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class StaticFile {
	public static final String HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";
	public static final String HTTP_DATE_GMT_TIMEZONE = "GMT";
	public static final int HTTP_CACHE_SECONDS = 60;
	private static Logger log = LoggerFactory.getLogger(StaticFile.class);
	public static Method DIRECTORY;
	public static Constructor SERVER_FILE_CONSTRUCTOR;
	private static final Pattern ALLOWED_FILE_NAME = Pattern.compile("[A-Za-z0-9][-_A-Za-z0-9\\.]*");
	private final File file;
	public static Method NOT_MODIFIED;
	public static Method SEND_FILE;
	private HttpServer server;
	private static Map<String, String> formats = new ConcurrentHashMap<>();

	static {
		try {
			DIRECTORY = StaticFile.class.getMethod("directory");
		} catch (NoSuchMethodException e) {
			log.error("Failed to initialize static file, directory method not found", e);
		}
		try {
			NOT_MODIFIED = StaticFile.class.getMethod("notModified");
		} catch (NoSuchMethodException e) {
			log.warn("Failed to initialize notModified method", e);
		}
		try {
			SEND_FILE = StaticFile.class.getMethod("sendFile", ChannelMessage.class);
		} catch (NoSuchMethodException e) {
			log.warn("Failed to initialize sendFile", e);
		}
		try {
			SERVER_FILE_CONSTRUCTOR = StaticFile.class.getConstructor(HttpServer.class, File.class);
		} catch (NoSuchMethodException e) {
			log.error("Failed to initialize static file, HttpRequest constructor not found", e);
		}
	}

	public StaticFile(HttpServer server, File file) {
		this.file = file;
		this.server = server;
		//htm,html -> text/html, json -> application/json, xml -> application/xml
		Map<String, String> textFormats = server.getConfig().files.custom_mime_types;
		//map multiple extensions to the same content type
		for (String commaSeparatedExtensions : textFormats.keySet()) {
			String[] extensions = commaSeparatedExtensions.split(",");
			String contentType = textFormats.get(commaSeparatedExtensions);
			for (String extension : extensions) {
				formats.put(extension, contentType);
			}
		}
	}

	/**
	 * When file timestamp is the same as what the browser is sending up, send a "304 Not Modified"
	 */
	public HttpResponse notModified() {
		HttpResponse response = new HttpResponse(HttpStatus.NOT_MODIFIED);
		setDateHeader(response);
		return response;
	}

	//it'll get sent to the client as is
	public HttpResponse directory() {
		return sendListing(file);
	}

	public Function sendFile(final ChannelMessage<HttpRequest> req) {
		try {
			MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap();
			String contentType = mimeTypesMap.getContentType(file.getPath());
			final RandomAccessFile raf = new RandomAccessFile(file, "r");
			final long fileLength = raf.length();
			final HttpResponse response = new HttpResponse();
			setContentLength(response, fileLength);
			//if its a supported text file then set to text mime type
			for (final String ext : formats.keySet()) {
				if (file.getName().endsWith(ext)) {
					contentType = formats.get(ext);
					break;
				}
			}
			//set mime type
			response.setHeader(CONTENT_TYPE, contentType);
			setDateAndCacheHeaders(response, file);
			HttpHeaders.setKeepAlive(response, false);
			//if (isKeepAlive(req.message)) {
			//	response.setHeader(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
			//}
			return new Function() {
				public void apply() {
					// Write the initial line and the header via the server's standard respond method
					server.respond(req.channel,response);
					// Write the content.
					ChannelFuture writeFuture = null;
					try {
						writeFuture = req.channel.write
								(new ChunkedFile
										(raf, 0, fileLength, server.getConfig().files.chunk_size));
					} catch (IOException e) {
						log.warn("Error writing chunk", e);
					}
					// Decide whether to close the connection or not.
					//if (!isKeepAlive(req.message)) {
					// Close the connection when the whole content is written out.
					writeFuture.addListener(ChannelFutureListener.CLOSE);
					//}
				}
			};
		} catch (FileNotFoundException fnfe) {
			//return null for 404
			return null;
		} catch (IOException e) {
			log.warn("Error reading file", e);
			return null;
		}
	}

	private void setDateAndCacheHeaders(HttpResponse response, File fileToCache) {
		SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
		dateFormatter.setTimeZone(TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE));

		// Date header
		Calendar time = new GregorianCalendar();
		response.setHeader(DATE, dateFormatter.format(time.getTime()));

		// Add cache headers
		time.add(Calendar.SECOND, HTTP_CACHE_SECONDS);
		response.setHeader(EXPIRES, dateFormatter.format(time.getTime()));
		response.setHeader(CACHE_CONTROL, "private, max-age=" + HTTP_CACHE_SECONDS);
		response.setHeader(
				LAST_MODIFIED, dateFormatter.format(new Date(fileToCache.lastModified())));
	}

	/**
	 * Sets the Date header for the HTTP response
	 *
	 * @param response HTTP response
	 */
	private void setDateHeader(io.netty.handler.codec.http.HttpResponse response) {
		SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
		dateFormatter.setTimeZone(TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE));

		Calendar time = new GregorianCalendar();
		response.setHeader(DATE, dateFormatter.format(time.getTime()));
	}

	private HttpResponse sendListing(File dir) {
		HttpResponse response = new HttpResponse();
		response.setHeader(CONTENT_TYPE, "text/html; charset=UTF-8");

		StringBuilder buf = new StringBuilder();
		String dirPath = dir.getPath();

		buf.append("<!DOCTYPE html>\r\n");
		buf.append("<html><head><title>");
		buf.append("Listing of: ");
		buf.append(dirPath);
		buf.append("</title></head><body>\r\n");

		buf.append("<h3>Listing of: ");
		buf.append(dirPath);
		buf.append("</h3>\r\n");

		buf.append("<ul>");
		buf.append("<li><a href=\"../\">..</a></li>\r\n");

		for (File f : dir.listFiles()) {
			if (f.isHidden() || !f.canRead()) {
				continue;
			}

			String name = f.getName();
			if (!ALLOWED_FILE_NAME.matcher(name).matches()) {
				continue;
			}

			buf.append("<li><a href=\"");
			buf.append(name);
			buf.append("\">");
			buf.append(name);
			buf.append("</a></li>\r\n");
		}

		buf.append("</ul></body></html>\r\n");
		ByteBuf buffer = Unpooled.copiedBuffer(buf, CharsetUtil.UTF_8);
		response.setContent(buffer);
		HttpHeaders.setContentLength(response, buffer.writerIndex());
		HttpHeaders.setKeepAlive(response, false);
		return response;
	}

}
