package io.higgs.http.server.protocol.media_type_decoders;

import com.fasterxml.jackson.databind.JsonNode;
import io.higgs.core.reflect.dependency.DependencyProvider;
import io.higgs.http.server.HttpRequest;
import io.higgs.http.server.WebApplicationException;
import io.higgs.http.server.protocol.MediaTypeDecoder;
import io.higgs.http.server.resource.JsonData;
import io.higgs.http.server.resource.MediaType;
import io.higgs.http.server.transformers.JsonTransformer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

/**
 * @author Courtney Robinson <courtney.robinson@datasift.com>
 */
public class JsonDecoder implements MediaTypeDecoder {
    private static final String UTF8 = "utf-8";
    ByteBuf content = Unpooled.buffer();
    DependencyProvider provider = new DependencyProvider();
    private HttpRequest request;

    public JsonDecoder(HttpRequest request) {
        this.request = request;
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
            node = JsonTransformer.mapper.readValue(json, JsonNode.class);
        } catch (IOException e) {
            throw new WebApplicationException(HttpResponseStatus.BAD_REQUEST, request, e);
        }
        provider.add(new JsonData(json, node));
    }

    @Override
    public DependencyProvider provider() {
        return provider;
    }
}
