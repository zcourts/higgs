package io.higgs.http.server.transformers;

import io.higgs.core.ConfigUtil;
import io.higgs.core.FileUtil;
import io.higgs.core.ResolvedFile;
import io.higgs.http.server.HttpRequest;
import io.higgs.http.server.HttpResponse;
import io.higgs.http.server.config.HttpConfig;
import io.higgs.http.server.protocol.HttpMethod;
import io.higgs.http.server.resource.MediaType;
import io.higgs.spi.ProviderFor;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
@ProviderFor(ResponseTransformer.class)
public class StaticFileTransformer extends BaseTransformer {
    private static Map<String, String> formats = new ConcurrentHashMap<>();
    private HttpConfig conf;
    private Pattern[] tlExtensions;

    public StaticFileTransformer() {
        conf = ConfigUtil.loadYaml("static_file_config.yml", HttpConfig.class);
        //htm,html -> text/html, json -> application/json, xml -> application/xml
        Map<String, String> textFormats = conf.files.custom_mime_types;
        //map multiple extensions to the same content type
        for (String commaSeparatedExtensions : textFormats.keySet()) {
            String[] extensions = commaSeparatedExtensions.split(",");
            String contentType = textFormats.get(commaSeparatedExtensions);
            for (String extension : extensions) {
                formats.put(extension, contentType);
            }
        }
        String[] tmp = conf.template_config.auto_parse_extensions.split(",");
        tlExtensions = new Pattern[tmp.length];
        for (int i = 0; i < tmp.length; i++) {
            String ext = tmp[i];
            if (ext != null && !ext.isEmpty()) {
                tlExtensions[i] = Pattern.compile(ext);
            }
        }
    }

    @Override
    public boolean canTransform(Object response, HttpRequest request, MediaType mediaType, HttpMethod method,
                                ChannelHandlerContext ctx) {
        return response != null && (response instanceof File ||
                response instanceof Path ||
                response instanceof InputStream);
    }

    @Override
    public void transform(Object response, HttpRequest request, HttpResponse res, MediaType mediaType,
                          HttpMethod method,
                          ChannelHandlerContext ctx) {
        if (response != null) {
            if (response instanceof File) {
                response = FileUtil.resolve((File) response);
            } else if (response instanceof Path) {
                response = FileUtil.resolve((Path) response);
            }
            if (response instanceof ResolvedFile) {
                writeResponseFromStream((ResolvedFile) response, res, request, mediaType, method, ctx);
            } else {
                res.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR);
                if (isError(response)) {
                    log.warn("Unexpected error to static file transformer", response);
                } else {
                    log.warn(String.format("Expecting an input stream or file,%s received",
                            response.getClass().getName()));
                }
            }
        }
    }

    private void writeResponseFromStream(ResolvedFile response, HttpResponse res, HttpRequest request,
                                         MediaType mediaType, HttpMethod method, ChannelHandlerContext ctx) {
        res.setManagedWriter(new StaticFileWriter(ctx, res, response, request, formats, conf));
    }

    @Override
    public ResponseTransformer instance() {
        return new StaticFileTransformer();
    }

    @Override
    public int priority() {
        return -1; //after JSON
    }
}
