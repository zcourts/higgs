package io.higgs.http.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.higgs.core.func.Function1;
import io.higgs.http.client.readers.Reader;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class JSONRequest extends Request<JSONRequest> {
    protected final ObjectMapper MAPPER = new ObjectMapper();
    protected Map<String, Object> dataMap = new HashMap<>();

    public JSONRequest(HttpRequestBuilder builder, EventLoopGroup group, URI uri, HttpVersion version, Reader f,
                       HttpMethod method) {
        super(builder, group, uri, method, version, f);
    }

    /**
     * @return a future representing the response eventually received
     * @throws java.lang.IllegalStateException if Jackson is unable to serialize the data added using
     *                                         {@link #addField(String, Object)}
     */
    public FutureResponse execute(Function1<Bootstrap> conf) {
        if (!request.headers().contains(HttpHeaders.Names.CONTENT_TYPE)) {
            request.headers().set(HttpHeaders.Names.CONTENT_TYPE, "application/json");
        }
        if (dataMap.size() > 0) {
            try {
                contents.writeBytes(Unpooled.wrappedBuffer(MAPPER.writeValueAsBytes(dataMap)));
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Unable to encode JSON data", e);
            }
        }
        request.headers().set(HttpHeaders.Names.CONTENT_LENGTH, contents.readableBytes());
        return super.execute(conf);
    }

    /**
     * Adds a field to the JSON payload that will be sent as the request body
     * The resulting payload will have all keys added with this method pointed to each value
     *
     * @param name  the name of the field
     * @param value the value the field should have
     * @return this
     * @throws java.lang.IllegalStateException if {@link #setData(Object)} has been used to set the data field on
     *                                         this request
     */
    public JSONRequest addField(String name, Object value) {
        if (contents.readableBytes() > 0) {
            throw new IllegalStateException("You cannot use addField in combination with setData");
        }
        dataMap.put(name, value);
        return this;
    }

    /**
     * Sets the content of the request.
     *
     * @param data the data to send in the request. If the type is a
     * @return this
     * @throws java.lang.IllegalStateException if {@link #addField(String, Object)} has been used to add any fields
     */
    public JSONRequest setData(String data) {
        if (dataMap != null) {
            throw new IllegalStateException("You cannot use setData in combination with addField");
        }
        if (data == null) {
            throw new IllegalArgumentException("data cannot be null");
        }
        contents.writeBytes(Unpooled.wrappedBuffer(data.getBytes(UTF8)));
        return this;
    }

    /**
     * Sets the content of the request.
     * NOTE: This will be immediately encoded to the JSON string which will become the contents of the request.
     *
     * @param data the data to send in the request. If the type is a
     * @return this
     * @throws java.lang.IllegalStateException if {@link #addField(String, Object)} has been used to add any fields
     */
    public JSONRequest setData(Object data) throws JsonProcessingException {
        if (dataMap != null) {
            throw new IllegalStateException("You cannot use setData in combination with addField");
        }
        if (data == null) {
            throw new IllegalArgumentException("data cannot be null");
        }
        contents.writeBytes(Unpooled.wrappedBuffer(MAPPER.writeValueAsBytes(data)));
        return this;
    }

    /**
     * @return The object mapper that will be used to encode the JSON data. This can be configured before executing
     * the request
     */
    public ObjectMapper mapper() {
        return MAPPER;
    }
}
