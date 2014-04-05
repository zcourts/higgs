package io.higgs.http.server.transformers;

import io.higgs.http.server.HttpRequest;
import io.higgs.http.server.HttpResponse;
import io.higgs.http.server.config.HttpConfig;
import io.higgs.http.server.protocol.HttpMethod;
import io.higgs.http.server.protocol.HttpProtocolConfiguration;
import io.higgs.http.server.resource.MediaType;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class StaticFileTransformer extends BaseTransformer {
    private static Map<String, String> formats = new ConcurrentHashMap<>();
    private final HttpProtocolConfiguration config;
    private final HttpConfig conf;
    private Pattern[] tlExtensions;

    public StaticFileTransformer(HttpProtocolConfiguration configuration) {
        this.config = configuration;
        conf = configuration.getServer().getConfig();
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
        return response != null && (response instanceof File || response instanceof JarFile);
    }

    @Override
    public void transform(Object response, HttpRequest request, HttpResponse res, MediaType mediaType,
                          HttpMethod method,
                          ChannelHandlerContext ctx) {
        //first try to match all thymeleaf extensions
        for (Pattern extensionPattern : tlExtensions) {
            String fileName = response instanceof File ?
                    ((File) response).getName() : ((JarFile) response).getEntry().getName();
            if (extensionPattern.matcher(fileName).matches()) {
                //parseTemplate(response, request, res, mediaType, method, ctx);
                //todo find outher service providers that match the extension
            }
        }
        if (response != null) {
            if (response instanceof InputStream) {
                writeResponseFromStream((InputStream) response, res, request, mediaType, method, ctx);
            } else if (response instanceof File) {
                writeResponseFromFile((File) response, res, request, mediaType, method, ctx, res.content());
            } else {
                res.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR);
                log.warn(String.format("Expecting an input stream or file,%s received", response.getClass().getName()));
            }
        }
    }

  /*  private void parseTemplate(Object response, HttpRequest request, HttpResponse httpResponse,
                               MediaType mediaType,
                               HttpMethod method, ChannelHandlerContext ctx) {

        ThymeleafTransformer transformer =
                new ThymeleafTransformer(conf.template_config, true);

        Path path = null;
        try {
            if (response instanceof JarFile) {
                //if it's a Jar file we need to create a tmp file from it
                //TODO Either find a way to make Thymeleaf accept a stream or cache tmp files
                JarFile file = (JarFile) response;
                path = Files.createTempFile("hs3-thymleaf" + file.getEntry().getName(), "tmpTpl");
                FileOutputStream out = new FileOutputStream(path.toFile());
                while (file.getInputStream().available() > 0) {
                    byte[] arr = new byte[file.getInputStream().available()];
                    file.getInputStream().read(arr);
                    out.write(arr);
                }
            } else {
                File file = (File) response;
                path = file.toPath();
            }
            String name = path.toString();
            //transformer uses getTemplate()
            method.setTemplate(name);
            transformer.transform(response, request, httpResponse, mediaType, method, ctx);
        } catch (IOException e) {
            log.warn(String.format("Error passing static file through Thymeleaf Path:%s", path), e);
            httpResponse.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }*/

    private void writeResponseFromStream(InputStream response, HttpResponse res, HttpRequest request,
                                         MediaType mediaType, HttpMethod method, ChannelHandlerContext ctx) {
        readEntireStream(response, res);
    }

    private void readEntireStream(InputStream response, HttpResponse res) {
        int b;
        try {
            while ((b = response.read()) != -1) {
                res.content().writeByte(b);
            }
        } catch (IOException e) {
            log.warn("Error reading file input stream", e);
        }
    }

    private void writeResponseFromFile(File file, final HttpResponse res, final HttpRequest request,
                                       MediaType mediaType,
                                       HttpMethod method, final ChannelHandlerContext ctx, ByteBuf buffer) {
        res.setManagedWriter(new StaticFileWriter(ctx, res, file, request, formats,conf));
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
