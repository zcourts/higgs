package io.higgs.http.server.providers;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.higgs.core.ConfigUtil;
import io.higgs.http.server.HttpRequest;
import io.higgs.http.server.HttpResponse;
import io.higgs.http.server.HttpStatus;
import io.higgs.http.server.protocol.HttpMethod;
import io.higgs.http.server.protocol.mediaTypeDecoders.JsonDecoder;
import io.higgs.http.server.resource.MediaType;
import io.higgs.http.server.providers.conf.JsonConfig;
import io.netty.channel.ChannelHandlerContext;

import javax.ws.rs.ext.Provider;

import static io.higgs.http.server.resource.MediaType.APPLICATION_JSON_TYPE;
import static io.higgs.http.server.providers.JsonResponseError.EMPTY_JSON_OBJECT;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
@Provider
public class JsonProvider extends BaseProvider {
    protected JsonConfig conf;

    public JsonProvider() {
        conf = ConfigUtil.loadYaml("json_config.yml", JsonConfig.class);
        setPriority(conf.priority);
        addSupportedTypes(APPLICATION_JSON_TYPE);
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
            JsonResponseError je = ((JsonResponseError) response);
            res.setStatus(je.getStatus());
            return je.getContent();
        }
        log.warn("Unable to convert exception to response", response);
        //never JSON encode an exception, user can set it as content to JsonResponseError if they want
        return EMPTY_JSON_OBJECT;
    }

    @Override
    public JsonProvider instance() {
        return this;//we can return this, instead of a new instance because the JSON transformer isn't stateful
    }
}
