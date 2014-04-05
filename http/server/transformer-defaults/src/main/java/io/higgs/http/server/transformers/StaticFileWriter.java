package io.higgs.http.server.transformers;

import io.higgs.http.server.HttpRequest;
import io.higgs.http.server.HttpResponse;
import io.higgs.http.server.HttpStatus;
import io.higgs.http.server.ManagedWriter;
import io.higgs.http.server.StaticFileMethod;
import io.higgs.http.server.config.HttpConfig;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelProgressiveFuture;
import io.netty.channel.ChannelProgressiveFutureListener;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.stream.ChunkedFile;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.MimetypesFileTypeMap;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Pattern;

import static io.netty.handler.codec.http.HttpHeaders.Names.CACHE_CONTROL;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaders.Names.DATE;
import static io.netty.handler.codec.http.HttpHeaders.Names.EXPIRES;
import static io.netty.handler.codec.http.HttpHeaders.Names.LAST_MODIFIED;
import static io.netty.handler.codec.http.HttpHeaders.isKeepAlive;
import static io.netty.handler.codec.http.HttpHeaders.setContentLength;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class StaticFileWriter implements ManagedWriter {
    protected static final Logger log = LoggerFactory.getLogger(StaticFileWriter.class);
    public static final String HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";
    public static final String HTTP_DATE_GMT_TIMEZONE = "GMT";
    public static final int HTTP_CACHE_SECONDS = 60;
    private static final Pattern ALLOWED_FILE_NAME = Pattern.compile("[A-Za-z0-9][-_A-Za-z0-9\\.]*");
    private final File file;
    private final ChannelHandlerContext ctx;
    private final io.netty.handler.codec.http.HttpResponse res = new DefaultHttpResponse(HTTP_1_1, OK);
    private final HttpResponse higgsPreparedResponse;
    private final HttpRequest request;
    private final HttpConfig conf;
    private File base;

    public boolean done;

    public StaticFileWriter(ChannelHandlerContext ctx, HttpResponse resIgnored, File file, HttpRequest request,
                            Map<String,
                                    String> formats, HttpConfig conf) {
        base = StaticFileMethod.baseUri(conf.files.public_directory);
        this.conf = conf;
        this.ctx = ctx;
        this.file = file;
        this.request = request;
        higgsPreparedResponse = resIgnored;
        if (file.isDirectory()) {
            sendListing();
            return;
        }
        MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap();
        String contentType = mimeTypesMap.getContentType(file.getPath());
        //if its a supported text file then set to text mime type
        for (final String ext : formats.keySet()) {
            if (file.getName().endsWith(ext)) {
                contentType = formats.get(ext);
                break;
            }
        }
        res.headers().set(CONTENT_TYPE, contentType);
        setDateAndCacheHeaders();
        if (isKeepAlive(request)) {
            res.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
        }
    }

    /**
     * If the file is a directory then the response object Higgs created is sent. Notice that
     * {@link HttpResponse} extends {@link io.netty.handler.codec.http.DefaultFullHttpResponse}
     * This means that once {@link #higgsPreparedResponse} is written Netty will close the stream.
     * <p/>
     * Because of this, {@link #res} is an instance of {@link DefaultHttpResponse} NOT THE {@link io.netty.handler
     * .codec.http.DefaultFullHttpResponse}
     * When {@link #res} is written, the stream is left open so that the contents of a file can be written after
     *
     * @return the final write future
     */
    public ChannelFuture doWrite() {
        if (file.isDirectory()) {
            //send full http response
            return ctx.writeAndFlush(higgsPreparedResponse);
        }
        //otherwise use an "incomplete" response
        ChannelFuture writeFuture;
        try {
            RandomAccessFile raf;
            try {
                raf = new RandomAccessFile(file, "r");
            } catch (FileNotFoundException fnfe) {
                res.setStatus(HttpStatus.NOT_FOUND);
                return ctx.writeAndFlush(res);
            }
            final long fileLength;
            try {
                fileLength = raf.length();
            } catch (IOException e) {
                log.warn("Error reading file", e);
                res.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
                return ctx.writeAndFlush(res);
            }
            res.setStatus(HttpStatus.OK);
            setContentLength(res, fileLength);
            ctx.write(res);
            writeFuture = ctx.write(new ChunkedFile(raf, 0, fileLength, conf.files.chunk_size),
                    ctx.newProgressivePromise());

            writeFuture.addListener(new ChannelProgressiveFutureListener() {
                @Override
                public void operationProgressed(ChannelProgressiveFuture future, long progress, long total) {
                    //TODO emit notification of file progress?
                }

                @Override
                public void operationComplete(ChannelProgressiveFuture future) throws Exception {
                    //mark as done sending
                    done = true;
                }
            });
            ChannelFuture lastWrite = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
            // Decide whether to close the connection or not.
            if (!isKeepAlive(request)) {
                // Close the connection when the whole content is written out.
                lastWrite.addListener(ChannelFutureListener.CLOSE);
            }
        } catch (IOException e) {
            done = true;
            log.warn("Error writing chunk", e);
            res.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
            writeFuture = ctx.writeAndFlush(res);
        }
        return writeFuture;
    }

    public boolean isDone() {
        return done;
    }

    private void setDateAndCacheHeaders() {
        SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
        dateFormatter.setTimeZone(TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE));

        // Date header
        Calendar time = new GregorianCalendar();
        res.headers().set(DATE, dateFormatter.format(time.getTime()));

        // Add cache headers
        time.add(Calendar.SECOND, HTTP_CACHE_SECONDS);
        res.headers().set(EXPIRES, dateFormatter.format(time.getTime()));
        res.headers().set(CACHE_CONTROL, "private, max-age=" + HTTP_CACHE_SECONDS);
        res.headers().set(
                LAST_MODIFIED, dateFormatter.format(new Date(file.lastModified())));
    }

    private void sendListing() {
        StringBuilder buf = new StringBuilder();
        String dirPath = file.getPath();
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

        for (File f : file.listFiles()) {
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
        higgsPreparedResponse.content().writeBytes(buffer);
        higgsPreparedResponse.headers().set(CONTENT_TYPE, "text/html; charset=UTF-8");
        HttpHeaders.setContentLength(res, buffer.writerIndex());
        HttpHeaders.setKeepAlive(res, false);
    }


}
