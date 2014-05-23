package io.higgs.http.server.providers;

import io.higgs.core.ConfigUtil;
import io.higgs.core.FileUtil;
import io.higgs.core.ResolvedFile;
import io.higgs.core.reflect.dependency.DependencyProvider;
import io.higgs.core.reflect.dependency.Injector;
import io.higgs.http.server.HttpRequest;
import io.higgs.http.server.HttpResponse;
import io.higgs.http.server.StaticFileMethod;
import io.higgs.http.server.WebApplicationException;
import io.higgs.http.server.config.HttpConfig;
import io.higgs.http.server.protocol.HttpMethod;
import io.higgs.http.server.providers.entity.BaseEntityProvider;
import io.higgs.http.server.resource.MediaType;
import io.higgs.http.server.providers.conf.FilesConfig;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.multipart.DiskAttribute;
import io.netty.handler.codec.http.multipart.DiskFileUpload;

import javax.ws.rs.ext.Provider;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
@Provider
public class StaticFileEntityProvider extends BaseEntityProvider {
    private static Map<String, String> formats = new ConcurrentHashMap<>();
    private FilesConfig conf;
    protected HttpConfig config;
    protected Path base;

    public StaticFileEntityProvider() {
        //inject HttpConfig
        Injector.inject(this, DependencyProvider.global());
        conf = ConfigUtil.loadYaml("static_file_config.yml", FilesConfig.class);
        base = Paths.get(config.public_directory);

        // should delete file
        DiskFileUpload.deleteOnExitTemporaryFile = conf.delete_temp_on_exit;
        // system temp directory
        DiskFileUpload.baseDirectory = conf.temp_directory;
        // should delete file on
        DiskAttribute.deleteOnExitTemporaryFile = conf.delete_temp_on_exit;
        // exit (in normal exit)
        DiskAttribute.baseDirectory = conf.temp_directory;

        //htm,html -> text/html, json -> application/json, xml -> application/xml
        Map<String, String> textFormats = conf.custom_mime_types;
        //map multiple extensions to the same content type
        for (String commaSeparatedExtensions : textFormats.keySet()) {
            String[] extensions = commaSeparatedExtensions.split(",");
            String contentType = textFormats.get(commaSeparatedExtensions);
            for (String extension : extensions) {
                formats.put(extension, contentType);
            }
        }
        setPriority(conf.priority); //after JSON
    }

    /**
     * Overrides the default behaviour in
     * {@link BaseEntityProvider#canTransform(Object, HttpRequest, MediaType, HttpMethod, ChannelHandlerContext)}
     * for a custom check on static files
     */
    @Override
    public boolean canTransform(Object response, HttpRequest request, MediaType mediaType, HttpMethod method,
                                ChannelHandlerContext ctx) {
        return isStaticFileResponse(response) ||
                //handle web application exceptions thrown by the static file method
                (response instanceof WebApplicationException &&
                        ((WebApplicationException) response).getSource() instanceof StaticFileMethod);
    }

    @Override
    public void transform(Object response, HttpRequest request, HttpResponse res, MediaType mediaType,
                          HttpMethod method,
                          ChannelHandlerContext ctx) {
        if (response != null) {
            if (response instanceof File) {
                response = FileUtil.resolve(base, (File) response);
            } else if (response instanceof Path) {
                response = FileUtil.resolve(base, (Path) response);
            } else if (response instanceof WebApplicationException) {
                WebApplicationException ex = (WebApplicationException) response;
                res.setStatus(ex.getStatus());
                if (ex.getMessage() != null && !ex.getMessage().isEmpty()) {
                    byte[] msg = ex.getMessage().getBytes();
                    ByteBuf buf = ctx.alloc().heapBuffer(msg.length);
                    res.resetContent(buf);
                }
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
        return new StaticFileEntityProvider();
    }
}
