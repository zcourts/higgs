package io.higgs.http.server.transformers;

import io.higgs.http.server.protocol.HttpMethod;
import io.higgs.http.server.protocol.HttpProtocolConfiguration;
import io.higgs.http.server.HttpRequest;
import io.higgs.http.server.HttpResponse;
import io.higgs.http.server.HttpStatus;
import io.higgs.http.server.StaticFileMethod;
import io.higgs.http.server.StaticFilePostWriteOperation;
import io.higgs.http.server.resource.MediaType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.stream.ChunkedFile;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import javax.activation.MimetypesFileTypeMap;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import static io.netty.handler.codec.http.HttpHeaders.Names.CACHE_CONTROL;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaders.Names.DATE;
import static io.netty.handler.codec.http.HttpHeaders.Names.EXPIRES;
import static io.netty.handler.codec.http.HttpHeaders.Names.LAST_MODIFIED;
import static io.netty.handler.codec.http.HttpHeaders.isKeepAlive;
import static io.netty.handler.codec.http.HttpHeaders.setContentLength;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class StaticFileTransformer extends BaseTransformer {
    private static Map<String, String> formats = new ConcurrentHashMap<>();
    private final HttpProtocolConfiguration config;
    public static final String HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";
    public static final String HTTP_DATE_GMT_TIMEZONE = "GMT";
    public static final int HTTP_CACHE_SECONDS = 60;
    private static final Pattern ALLOWED_FILE_NAME = Pattern.compile("[A-Za-z0-9][-_A-Za-z0-9\\.]*");
    private File base;

    public StaticFileTransformer(HttpProtocolConfiguration configuration) {
        this.config = configuration;
        base = StaticFileMethod.baseUri(config.getServer().getConfig().files.public_directory);
        //htm,html -> text/html, json -> application/json, xml -> application/xml
        Map<String, String> textFormats = configuration.getServer().getConfig().files.custom_mime_types;
        //map multiple extensions to the same content type
        for (String commaSeparatedExtensions : textFormats.keySet()) {
            String[] extensions = commaSeparatedExtensions.split(",");
            String contentType = textFormats.get(commaSeparatedExtensions);
            for (String extension : extensions) {
                formats.put(extension, contentType);
            }
        }
    }

    @Override
    public boolean canTransform(Object response, HttpRequest request, MediaType mediaType, HttpMethod method,
                                ChannelHandlerContext ctx) {
        return response != null && (response instanceof File || response instanceof InputStream);
    }

    @Override
    public HttpResponse transform(Object response, HttpRequest request, MediaType mediaType, HttpMethod method,
                                  ChannelHandlerContext ctx) {
        HttpResponse res = new HttpResponse(HttpResponseStatus.OK);
        if (response != null) {
            if (response instanceof InputStream) {
                writeResponseFromStream((InputStream) response, res, request, mediaType, method, ctx);
            } else if (response instanceof File) {
                ByteBuf buffer = ctx.alloc().buffer();
                res = new HttpResponse(buffer);
                writeResponseFromFile((File) response, res, request, mediaType, method, ctx, buffer);
            } else {
                res.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR);
                log.warn(String.format("Expecting an input stream or file,%s received", response.getClass().getName()));
            }
        }
        return res;
    }

    private void writeResponseFromStream(InputStream response, HttpResponse res, HttpRequest request,
                                         MediaType mediaType, HttpMethod method, ChannelHandlerContext ctx) {
        int b;
        try {
            while ((b = response.read()) != -1) {
                res.content().writeByte(b);
            }
        } catch (IOException e) {
            log.warn("Error reading file input stream", e);
        }
    }

    private void writeResponseFromFile(File file, HttpResponse res, final HttpRequest request, MediaType mediaType,
                                       HttpMethod method, final ChannelHandlerContext ctx, ByteBuf buffer) {
        if (file.isDirectory()) {
            sendListing(res, file);
            return;
        }
        MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap();
        String contentType = mimeTypesMap.getContentType(file.getPath());
        final RandomAccessFile raf;
        try {
            raf = new RandomAccessFile(file, "r");
        } catch (FileNotFoundException fnfe) {
            res.setStatus(HttpStatus.NOT_FOUND);
            return;
        }
        final long fileLength;
        try {
            fileLength = raf.length();
        } catch (IOException e) {
            log.warn("Error reading file", e);
            res.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
            return;
        }
        res.setStatus(HttpStatus.OK);
        setContentLength(res, fileLength);
        //if its a supported text file then set to text mime type
        for (final String ext : formats.keySet()) {
            if (file.getName().endsWith(ext)) {
                contentType = formats.get(ext);
                break;
            }
        }
        res.headers().set(CONTENT_TYPE, contentType);
        setDateAndCacheHeaders(res, file);
        if (isKeepAlive(request)) {
            res.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
        }
        res.setPostWriteOp(new StaticFilePostWriteOperation() {
            public boolean done;

            public void apply() {
                try {
                    final ChannelFuture writeFuture =
                            ctx.write(new ChunkedFile(raf, 0, fileLength,
                                    config.getServer().getConfig().files.chunk_size));
                    writeFuture.addListener(new GenericFutureListener<Future<Void>>() {
                        public void operationComplete(Future<Void> future) throws Exception {
                            //mark as done sending
                            ctx.write(LastHttpContent.EMPTY_LAST_CONTENT);
                            done = true;
                            if (!isKeepAlive(request)) {
                                writeFuture.channel().close();
                            }
                        }
                    });
                } catch (IOException e) {
                    done = true;
                    log.warn("Error writing chunk", e);
                    HttpResponse res = new HttpResponse(HttpStatus.INTERNAL_SERVER_ERROR);
                    ctx.write(res);
                }
            }

            public boolean isDone() {
                return done;
            }
        });
    }

    private void setDateAndCacheHeaders(HttpResponse response, File fileToCache) {
        SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
        dateFormatter.setTimeZone(TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE));

        // Date header
        Calendar time = new GregorianCalendar();
        response.headers().set(DATE, dateFormatter.format(time.getTime()));

        // Add cache headers
        time.add(Calendar.SECOND, HTTP_CACHE_SECONDS);
        response.headers().set(EXPIRES, dateFormatter.format(time.getTime()));
        response.headers().set(CACHE_CONTROL, "private, max-age=" + HTTP_CACHE_SECONDS);
        response.headers().set(
                LAST_MODIFIED, dateFormatter.format(new Date(fileToCache.lastModified())));
    }

    private void sendListing(HttpResponse response, File dir) {
        StringBuilder buf = new StringBuilder();
        String dirPath = dir.getPath();
        dirPath = dirPath.replace(base.getPath(), "");
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
            buf.append(dirPath + "/" + name);
            buf.append("\">");
            buf.append(name);
            buf.append("</a></li>\r\n");
        }

        buf.append("</ul></body></html>\r\n");
        ByteBuf buffer = Unpooled.copiedBuffer(buf, CharsetUtil.UTF_8);
        response.content().writeBytes(buffer);
        response.headers().set(CONTENT_TYPE, "text/html; charset=UTF-8");
        HttpHeaders.setContentLength(response, buffer.writerIndex());
        HttpHeaders.setKeepAlive(response, false);
    }

    @Override
    public ResponseTransformer instance() {
        return new StaticFileTransformer(config);
    }

    @Override
    public int priority() {
        return -1; //after JSON
    }
}
