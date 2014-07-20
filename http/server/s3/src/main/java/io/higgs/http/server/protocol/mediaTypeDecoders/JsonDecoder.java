package io.higgs.http.server.protocol.mediaTypeDecoders;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import io.higgs.core.reflect.dependency.DependencyProvider;
import io.higgs.http.server.HttpRequest;
import io.higgs.http.server.protocol.MediaTypeDecoder;
import io.higgs.http.server.resource.JsonData;
import io.higgs.http.server.resource.MediaType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpResponseStatus;

import javax.ws.rs.WebApplicationException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

/**
 * @author Courtney Robinson <courtney.robinson@datasift.com>
 */
public class JsonDecoder implements MediaTypeDecoder {
    public static final ObjectMapper mapper = new ObjectMapper();
    private static final String UTF8 = "utf-8";
    ByteBuf content = Unpooled.buffer();
    DependencyProvider provider = new DependencyProvider();
    private HttpRequest request;

    public JsonDecoder(HttpRequest request) {
        this.request = request;
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

    public boolean canDecode(List<MediaType> mediaType) {
        if (mediaType == null || mediaType.size() == 0) {
            return false;
        }
        for (MediaType m : mediaType) {
            if (MediaType.APPLICATION_JSON_TYPE.isCompatible(m)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void offer(HttpContent chunk) {
        content.writeBytes(chunk.content());
    }

    @Override
    public void finished(ChannelHandlerContext ctx) {
        byte[] bytes = new byte[content.readableBytes()];
        content.readBytes(bytes);
        String json = new String(bytes, Charset.forName(UTF8));
        JsonNode node;
        try {
            node = mapper.readValue(json, JsonNode.class);
        } catch (IOException e) {
            throw new WebApplicationException(HttpResponseStatus.BAD_REQUEST.code());
        }
        provider.add(new JsonData(json, node));
    }

    @Override
    public DependencyProvider provider() {
        return provider;
    }
}
