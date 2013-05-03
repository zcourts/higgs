package io.higgs.http.server.protocol;

import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class StaticFileMethod extends HttpMethod {

    private static final Pattern INSECURE_URI = Pattern.compile(".*[<>&\"].*");
    public static final String HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";
    public static final String HTTP_DATE_GMT_TIMEZONE = "GMT";
    public static final int HTTP_CACHE_SECONDS = 60;
    private final HttpProtocolConfiguration config;
    private File base;
    private boolean canServe = true;
    private static Logger log = LoggerFactory.getLogger(StaticFileMethod.class);
    private static final Method METHOD;

    static {
        try {
            METHOD = StaticFileMethod.class.getMethod("getFile");
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Unable to set up static file method", e);
        }
    }

    private InputStream zipStream;
    private File matchedFile;

    public StaticFileMethod(HttpProtocolConfiguration protocolConfig) {
        super(StaticFileMethod.class, METHOD);
        this.config = protocolConfig;
        base = baseUri(config.getServer().getConfig().files.public_directory);
        if (base != null) {
            if (!base.exists()) {
                canServe = false;
                log.warn("Public files directory that is configured does not exist. Will not serve static files");
            }
        }
    }

    public static File baseUri(String public_directory) {
        URL uri = Thread.currentThread().getContextClassLoader().getResource(public_directory);
        File file = null;
        if (uri != null) {
            try {
                file = new File(uri.toURI());
            } catch (URISyntaxException e) {
                log.info("", e);
            }
        }
        if (file == null) {
            file = new File(public_directory);
            if (!file.exists()) {
                log.warn("Public files directory that is configured does not exist. Will not serve static files");
            }
        }
        return file;
    }

    @Override
    public boolean matches(String path, ChannelHandlerContext ctx, Object msg) {
        if (!(msg instanceof HttpRequest)) {
            return false;
        }
        HttpRequest request = (HttpRequest) msg;
        //three main conditions need to be met to serve a static file
        //is this a GET request?
        //does the base directory exist?
        //is the URL a subdirectory of the base?
        if (!canServe || !request.getMethod().name().equalsIgnoreCase(
                io.netty.handler.codec.http.HttpMethod.GET.name())) {
            return false;
        }
        String base_dir = config.getServer().getConfig().files.public_directory;
        String uri = normalizeURI(request, base_dir);
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
                    boolean fromJar = returnJarStreamEndpoint(url);
                    if (fromJar) {
                        return fromJar;
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
                return false;
            }
        }
        //if its not a sub directory tell them no!
        if (!isSubDirectory(base, file)) {
            return false;
        }
        if (file.isDirectory()) {
            //get list of files, if index/default found then send it instead of listing directory
            try {
                DirectoryStream<Path> paths = Files.newDirectoryStream(file.toPath());
                boolean list = true;
                for (Path localPath : paths) {
                    Path name = localPath.getFileName();
                    if (config.getServer().getConfig().files.index_file.endsWith(name.toString())) {
                        file = localPath.toFile();
                        list = false;
                        break;
                    }
                }
                if (list) {
                    if (config.getServer().getConfig().files.enable_directory_listing) {
                        this.matchedFile = file;
                        return true;
                    } else {
                        return false; //directory listing not enabled return 404 or another error
                    }
                }
            } catch (IOException e) {
                log.info(String.format("Failed to list files in directory {%s}", e.getMessage()));
                return false;
            }
        }
        if (!file.isFile()) {
            return false;
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
        this.matchedFile = file;
        return true;
    }

    private String normalizeURI(HttpRequest request, String base_dir) {
        String uri = request.getUri();
        //remove query string from path
        if (uri.contains("?")) {
            uri = uri.substring(0, uri.indexOf("?"));
        }
        if (uri.equals("/") && config.getServer().getConfig().files.serve_index_file) {
            uri = config.getServer().getConfig().files.index_file;
        }
        //the URL must be from the public directory
        if (!uri.startsWith("/")) {
            uri = "/" + uri;
        }
        if (base_dir.endsWith("/")) {
            uri = uri.substring(1);
        }
        return uri;
    }

    public Object getFile() {
        if (zipStream != null) {
            return zipStream;
        }
        return matchedFile;
    }

    public Object invoke(ChannelHandlerContext ctx, String path, Object msg, Object[] params)
            throws InvocationTargetException, IllegalAccessException, InstantiationException {
        return classMethod.invoke(this, params);
    }

    private boolean returnJarStreamEndpoint(final String url) throws IOException {
        //return null;
        //todo support  serving files from JARs
        //need to implement or extend StaticFile so that it accepts an input stream.
        //then modify sendFile method to send a netty ChunkedStream passing in the jar input stream
        String jar = url.substring(url.indexOf(":") + 1, url.indexOf("!"));
        String jarFile = url.substring(url.indexOf("!") + 1);
        ZipFile zip = new ZipFile(jar);
        Enumeration<? extends ZipEntry> entries = zip.entries();
        if (entries != null) {
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (jarFile.equalsIgnoreCase(entry.getName())) {
                    zipStream = zip.getInputStream(entry);
                    return true;
                }
            }
        }
        return false;
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
        return Integer.MIN_VALUE;
    }
}
