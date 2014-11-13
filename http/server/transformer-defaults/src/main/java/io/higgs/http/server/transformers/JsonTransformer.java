package io.higgs.http.server.transformers;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.higgs.core.ConfigUtil;
import io.higgs.http.server.HttpRequest;
import io.higgs.http.server.HttpResponse;
import io.higgs.http.server.HttpStatus;
import io.higgs.http.server.protocol.HttpMethod;
import io.higgs.http.server.protocol.mediaTypeDecoders.JsonDecoder;
import io.higgs.http.server.resource.MediaType;
import io.higgs.http.server.transformers.conf.JsonConfig;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.kohsuke.MetaInfServices;

import java.net.URI;
import java.net.URISyntaxException;

import static io.higgs.http.server.resource.MediaType.APPLICATION_JSON_TYPE;
import static io.higgs.http.server.transformers.JsonResponseError.EMPTY_JSON_OBJECT;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
//@ProviderFor(ResponseTransformer.class)
@MetaInfServices(ResponseTransformer.class)
public class JsonTransformer extends BaseTransformer {
    protected JsonConfig conf;

    public JsonTransformer() {
        conf = ConfigUtil.loadYaml("json_config.yml", JsonConfig.class);
        setPriority(conf.priority);
        addSupportedTypes(APPLICATION_JSON_TYPE);
    }

    @Override
    public boolean canTransform(Object response, HttpRequest request, MediaType mediaType,
                                HttpMethod method, ChannelHandlerContext ctx) {
        return method != null && pathIsJson(request.getUri())
                || super.canTransform(response, request, mediaType, method, ctx);
    }

    private boolean pathIsJson(String uri) {
        if (uri == null || uri.isEmpty()) {
            return false;
        }
        try {
            String path = new URI(uri).getPath();
            if (path == null || path.isEmpty()) {
                return false;
            }
            if (path.endsWith(".json")) {
                return true;
            }
        } catch (URISyntaxException e) {
            return false;
        }
        return false;
    }

    @Override
    public void transform(Object response, HttpRequest request, HttpResponse res, MediaType mediaType,
                          HttpMethod method,
                          ChannelHandlerContext ctx) {
        byte[] data = null;
        if (response == null) {
            data = EMPTY_JSON_OBJECT.getBytes();
        } else {
            if (isError(response)) {
                response = convertErrorToResponseObject(res, (Throwable) response);
            }
            try {
                data = JsonDecoder.mapper.writeValueAsBytes(response);
            } catch (JsonProcessingException e) {
                log.warn("Unable to transform response to JSON", e);
                res.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        setResponseContent(res, data);
    }

    protected Object convertErrorToResponseObject(HttpResponse res, Throwable response) {
        determineErrorStatus(res, response);
        if (response instanceof JsonResponseError) {
            JsonResponseError je = (JsonResponseError) response;
            res.setStatus(HttpResponseStatus.valueOf(je.getResponse().getStatus()));
            return je.getContent();
        }
        log.warn("Unable to convert exception to response", response);
        //never JSON encode an exception, user can set it as content to JsonResponseError if they want
        return EMPTY_JSON_OBJECT;
    }

    @Override
    public JsonTransformer instance() {
        return this; //we can return this, instead of a new instance because the JSON transformer isn't stateful
    }
}
