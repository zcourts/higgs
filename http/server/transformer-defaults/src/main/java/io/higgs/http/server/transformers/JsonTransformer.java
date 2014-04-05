package io.higgs.http.server.transformers;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.higgs.http.server.BaseTransformer;
import io.higgs.http.server.HttpRequest;
import io.higgs.http.server.HttpResponse;
import io.higgs.http.server.HttpStatus;
import io.higgs.http.server.TransformerType;
import io.higgs.http.server.protocol.HttpMethod;
import io.higgs.http.server.protocol.mediaTypeDecoders.JsonDecoder;
import io.higgs.http.server.resource.MediaType;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.io.File;
import java.io.InputStream;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class JsonTransformer extends BaseTransformer {
    @Override
    public boolean canTransform(Object response, HttpRequest request, MediaType mediaType,
                                HttpMethod method, ChannelHandlerContext ctx) {
        if (response != null && !(response instanceof File || response instanceof InputStream)) {
            for (MediaType type : request.getAcceptedMediaTypes()) {
                if (type.isCompatible(MediaType.APPLICATION_JSON_TYPE)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void transform(Object response, HttpRequest request, HttpResponse httpResponse, MediaType mediaType,
                          HttpMethod method,
                          ChannelHandlerContext ctx) {
        transform(response, request, httpResponse, mediaType, method, ctx, null);
    }

    @Override
    public JsonTransformer instance() {
        return new JsonTransformer();
    }

    @Override
    public TransformerType[] supportedTypes() {
        return new TransformerType[]{TransformerType.GENERIC};
    }

    public void transform(Object response, HttpRequest request, HttpResponse res, MediaType mediaType,
                          HttpMethod method,
                          ChannelHandlerContext ctx, HttpResponseStatus status) {
        byte[] data = null;
        if (response == null) {
            data = "{}".getBytes();
        } else {
            try {
                data = JsonDecoder.mapper.writeValueAsBytes(response);
            } catch (JsonProcessingException e) {
                log.warn("Unable to transform response to JSON", e);
                //todo use template for 500
                res.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        if (data != null) {
            res.setStatus(status == null ? HttpStatus.OK : status);
            res.content().writeBytes(data);
            HttpHeaders.setContentLength(res, data.length);
        }
    }

    @Override
    public int priority() {
        //goes after the thymeleaf transformer so that wild card requests are assumed to handle HTML if
        //the end  point as a template
        return 0;
    }
}
