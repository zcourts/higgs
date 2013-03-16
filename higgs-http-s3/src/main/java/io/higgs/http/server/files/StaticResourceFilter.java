package io.higgs.http.server.files;

import io.higgs.http.server.DefaultResourceFilter;
import io.higgs.http.server.Endpoint;
import io.higgs.http.server.HttpRequest;
import io.higgs.http.server.HttpServer;
import io.netty.handler.codec.http.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

/**
 * todo this class and the rest of this package needs refactoring and cleaning up like there's no tomorrow!
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class StaticResourceFilter extends DefaultResourceFilter {
    private static final Pattern INSECURE_URI = Pattern.compile(".*[<>&\"].*");
    public static final String HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";
    public static final String HTTP_DATE_GMT_TIMEZONE = "GMT";
    public static final int HTTP_CACHE_SECONDS = 60;
    private File base;
    private boolean canServe = true;
    private HttpServer server;
    private Logger log = LoggerFactory.getLogger(getClass());

    public StaticResourceFilter(final HttpServer server) {
        super(server);
        this.server = server;
        URL uri = Thread.currentThread().getContextClassLoader().getResource(
                server.getConfig().files.public_directory
        );
        String msg = "Public files directory that is configured does not exist. Will not serve static files";
        if (uri != null) {
            try {
                base = new File(uri.toURI());
                if (!base.exists()) {
                    canServe = false;
                    log.warn(msg);
                }
            } catch (URISyntaxException e) {
                log.info("", e);
            }
        }
        if (base == null) {
            base = new File(server.getConfig().files.public_directory);
            if (!base.exists()) {
                canServe = false;
                log.warn(msg);
            }
        }
    }

    @Override
    public Endpoint getEndpoint(final HttpRequest request) {
        //three main conditions need to be met to serve a static file
        //is this a GET request?
        //does the base directory exist?
        //is the URL a subdirectory of the base?
        if (!canServe || !request.getMethod().name().equalsIgnoreCase(HttpMethod.GET.name())) {
            return null;
        }
        String base_dir = server.getConfig().files.public_directory;
        String uri = request.getUri();
        //remove query string from path
        if (uri.contains("?")) {
            uri = uri.substring(0, uri.indexOf("?"));
        }
        if (uri.equals("/") && server.getConfig().files.serve_index_file) {
            uri = server.getConfig().files.index_file;
        }
        //the URL must be from the public directory
        if (!uri.startsWith("/")) {
            uri = "/" + uri;
        }
        if (base_dir.endsWith("/")) {
            uri = uri.substring(1);
        }
        //sanitize before use
        uri = base_dir + sanitizeUri(uri);
        File file = null;
        //check the classpath first
        URL source = Thread.currentThread().getContextClassLoader().getResource(uri);
        try {
            if (source != null) {
                //jar:file:/B:/dev/projects/Higgs/higgs-http-s3/target/higgs-http-3s-0.0.1-SNAPSHOT.jar
                //!/public/default.html
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
                    if (file.isHidden() || !file.exists()) {
                        file = null;
                    }
                }
            }
        } catch (Throwable e) {
            log.debug("", e);
        }
        //if we couldn't load it from the class path then try to get it from disk
        if (file == null) {
            file = new File(uri);
            if (!file.isDirectory() && (file.isHidden() || !file.exists())) {
                return null;
            }
        }
        //if its not a sub directory tell them no!
        if (!isSubDirectory(base, file)) {
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
                        return null; //directory listing not enabled return 404 or another error
                    }
                }
            } catch (IOException e) {
                log.info(String.format("Failed to list files in directory {%s}", e.getMessage()));
                return null;
            }
        }
        if (!file.isFile()) {
            return null;
        }
        // Cache Validation
//        String modDate = request.headers().get(IF_MODIFIED_SINCE);
//        if (modDate != null && !modDate.isEmpty()) {
//            SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
//            Date ifModifiedSinceDate = null;
//            try {
//                ifModifiedSinceDate = dateFormatter.parse(modDate);
//                // Only compare up to the second because the datetime format we send to the client
//                // does not have milliseconds
//                long ifModifiedSinceDateSeconds = ifModifiedSinceDate.getTime() / 1000;
//                long fileLastModifiedSeconds = file.lastModified() / 1000;
//                if (ifModifiedSinceDateSeconds == fileLastModifiedSeconds) {
//                    return new Endpoint("" + System.nanoTime(), server, StaticFile.class, StaticFile.NOT_MODIFIED,
//                            StaticFile.SERVER_FILE_CONSTRUCTOR, server, file);
//                }
//            } catch (ParseException e) {
//                log.warn("Unable to parse modified since date, ignoring and sending file...");
//            }
//        }
        return new Endpoint("" + System.nanoTime(), server, StaticFile.class, StaticFile.SEND_FILE,
                StaticFile.SERVER_FILE_CONSTRUCTOR, server, file);
    }

    private Endpoint returnJarStreamEndpoint(final String url) throws IOException {
        return null;
        //todo support  serving files from JARs
        //need to implement or extend StaticFile so thatit accepts an input stream.
        //then modify sendFile method to send a netty ChunkedStream passing in the jar input stream
//        String jar = url.substring(url.indexOf(":") + 1, url.indexOf("!"));
//        String jarFile = url.substring(url.indexOf("!") + 1);
//        ZipFile zip = new ZipFile(jar);
//        if (zip != null) {
//            Enumeration<? extends ZipEntry> entries = zip.entries();
//            if (entries != null) {
//                while (entries.hasMoreElements()) {
//                    ZipEntry entry = entries.nextElement();
//                    if (jarFile.equalsIgnoreCase(entry.getName())) {
////                                    zip.getInputStream()
//                    }
//                }
//            }
//        }
    }

    /**
     * Checks, whether the child directory is a subdirectory of the base
     * directory.
     * java2s.com/Tutorial/Java/0180__File/Checkswhetherthechilddirectoryisasubdirectoryofthebasedirectory.htm
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
            log.debug("", e);
        }
        return false;
    }

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

    @Override
    public int priority() {
        return 0;
    }
}
