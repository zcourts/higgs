package io.higgs.http.server.transformers;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import io.higgs.http.server.HttpRequest;
import io.higgs.http.server.HttpResponse;
import io.higgs.http.server.HttpStatus;
import io.higgs.http.server.protocol.HttpMethod;
import io.higgs.http.server.resource.MediaType;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class JsonTransformer extends BaseTransformer {
    private Logger log = LoggerFactory.getLogger(getClass());
    public static final ObjectMapper mapper = new ObjectMapper();

    public JsonTransformer() {
        mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, true);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true);
        mapper.registerModule(new JodaModule());
        //auto discover fields
        VisibilityChecker visibilityChecker = mapper.getSerializationConfig().getDefaultVisibilityChecker();
        visibilityChecker.withFieldVisibility(JsonAutoDetect.Visibility.ANY);
        visibilityChecker.withGetterVisibility(JsonAutoDetect.Visibility.ANY);
        visibilityChecker.withSetterVisibility(JsonAutoDetect.Visibility.ANY);
        visibilityChecker.withCreatorVisibility(JsonAutoDetect.Visibility.ANY);
        mapper.setVisibilityChecker(visibilityChecker);
    }

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

    public void transform(Object response, HttpRequest request, HttpResponse res, MediaType mediaType,
                          HttpMethod method,
                          ChannelHandlerContext ctx, HttpResponseStatus status) {
        byte[] data = null;
        if (response == null) {
            data = "{}".getBytes();
        } else {
            try {
                data = mapper.writeValueAsBytes(response);
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
