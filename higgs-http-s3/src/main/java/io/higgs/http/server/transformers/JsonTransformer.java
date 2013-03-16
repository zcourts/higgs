package io.higgs.http.server.transformers;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import io.higgs.http.server.HttpRequest;
import io.higgs.http.server.HttpResponse;
import io.higgs.http.server.HttpServer;
import io.higgs.http.server.HttpStatus;
import io.higgs.http.server.ResponseTransformer;
import io.higgs.http.server.resource.MediaType;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.PriorityBlockingQueue;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class JsonTransformer extends BaseTransformer {
    private Logger log = LoggerFactory.getLogger(getClass());
    protected final ObjectMapper mapper = new ObjectMapper();

    public JsonTransformer() {
        mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, true);
        //auto discover fields
        VisibilityChecker visibilityChecker = mapper.getSerializationConfig().getDefaultVisibilityChecker();
        visibilityChecker.withFieldVisibility(JsonAutoDetect.Visibility.ANY);
        visibilityChecker.withGetterVisibility(JsonAutoDetect.Visibility.ANY);
        visibilityChecker.withSetterVisibility(JsonAutoDetect.Visibility.ANY);
        visibilityChecker.withCreatorVisibility(JsonAutoDetect.Visibility.ANY);
        mapper.setVisibilityChecker(visibilityChecker);
    }

    @Override
    public boolean canTransform(Object response, HttpRequest request) {
        for (MediaType type : request.getMediaTypes()) {
            if (type.isCompatible(MediaType.APPLICATION_JSON_TYPE)
                    || type.isCompatible(MediaType.TEXT_PLAIN_TYPE)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public HttpResponse transform(HttpServer server, final Object returns, HttpRequest request,
                                  PriorityBlockingQueue<ResponseTransformer> registeredTransformers) {
        return transform(server, returns, request, registeredTransformers, null);
    }

    public HttpResponse transform(HttpServer server, Object returns, HttpRequest request,
                                  PriorityBlockingQueue<ResponseTransformer> transformers,
                                  HttpResponseStatus status) {
        byte[] data;
        if (returns == null) {
            data = "{}".getBytes();
        } else {
            try {
                data = mapper.writeValueAsBytes(returns);
            } catch (JsonProcessingException e) {
                log.warn("Unable to transform response to JSON", e);
                //todo use template for 500
                return new HttpResponse(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        if (data != null) {
            HttpResponse response = new HttpResponse(request.getProtocolVersion(),
                    status == null ? HttpStatus.OK : status,
                    Unpooled.wrappedBuffer(data));
            HttpHeaders.setContentLength(response, data.length);
            return response;
        } else {
            return tryNextTransformer(server, returns, request, transformers);
        }
    }

    @Override
    public int priority() {
        //goes after the thymeleaf transformer so that wild card requests are assumed to handle HTML if
        //the end  point as a template
        return 0;
    }
}
