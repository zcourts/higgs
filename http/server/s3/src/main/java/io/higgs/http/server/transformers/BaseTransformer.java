package io.higgs.http.server.transformers;

import io.higgs.core.ResolvedFile;
import io.higgs.http.server.HttpRequest;
import io.higgs.http.server.HttpResponse;
import io.higgs.http.server.protocol.HttpMethod;
import io.higgs.http.server.resource.MediaType;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public abstract class BaseTransformer implements ResponseTransformer {
    protected Logger log = LoggerFactory.getLogger(getClass());
    protected int priority;
    protected Set<MediaType> supportedTypes = new HashSet<>();

    protected void setResponseContent(HttpResponse res, byte[] data) {
        if (data != null) {
            res.content().writeBytes(data);
            HttpHeaders.setContentLength(res, data.length);
        }
    }

    protected void determineErrorStatus(HttpResponse res, Throwable response) {
        HttpResponseStatus status = null;
        if (response == null) {
            status = HttpResponseStatus.NO_CONTENT;
        } else if (response instanceof WebApplicationException) {
            status = HttpResponseStatus.valueOf(((WebApplicationException) response).getResponse().getStatus());
        }
        res.setStatus(status);
    }

    @Override
    public int setPriority(int value) {
        int old = priority;
        priority = value;
        return old;
    }

    @Override
    public int priority() {
        return priority;
    }

    @Override
    public int compareTo(ResponseTransformer that) {
        return that.priority() - this.priority();
    }

    @Override
    public boolean canTransform(Object response, HttpRequest request, MediaType mediaType,
                                HttpMethod method, ChannelHandlerContext ctx) {
        if (method.hasProduces()) {
            return method.produces(supportedTypes.toArray(new MediaType[supportedTypes.size()]));
        } else {
            //if is error we need to handle converting the error to an output or if the response isn't a static file
            return mediaTypeMatches(request) && ((isError(response)) || !isStaticFileResponse(response));
        }
    }

    /**
     * Checks if the this transformer supports the {@link io.higgs.http.server.resource.MediaType}s supported by the
     * request.
     *
     * @param request the request to check against this transformer's supported types
     * @return true if at the request and transformer have at least one type in common.
     * NOTE: If request is null this returns true
     */
    public boolean mediaTypeMatches(HttpRequest request) {
        if (request == null) {
            return true;
        }
        for (MediaType type : request.getAcceptedMediaTypes()) {
            for (MediaType supportedType : supportedTypes) {
                if (type.isCompatible(supportedType)) {
                    return true;
                }
            }
        }
        return false;
    }

    protected boolean isError(Object response) {
        return response instanceof Throwable;
    }

    /**
     * Checks if a the given object is one of the types considered to be a static file response. Currently the following
     * {@link java.io.File}|{@link io.higgs.core.ResolvedFile}|{@link java.io.InputStream}|{@link java.nio.file.Path}
     *
     * @param response the response object to check
     * @return true if the type is considered a static file resource
     */
    public boolean isStaticFileResponse(Object response) {
        return response instanceof File
                || response instanceof ResolvedFile
                || response instanceof InputStream
                || response instanceof Path;
    }

    /**
     * Adds one or more {@link io.higgs.http.server.resource.MediaType}s
     *
     * @param type at least one media type to add
     */
    public void addSupportedTypes(MediaType... type) {
        if (type != null) {
            Collections.addAll(supportedTypes, type);
        }
    }
}
