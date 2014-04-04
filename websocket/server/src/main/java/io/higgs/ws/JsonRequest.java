package io.higgs.ws;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple web socket event with a path and message property.
 * path is the method to be invoked on either the client or server
 * message is a JSON string object
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class JsonRequest {
    @JsonIgnore
    protected final Logger log = LoggerFactory.getLogger(getClass());
    @JsonIgnore
    protected String rawString;
    @JsonProperty
    protected JsonNode message;
    @JsonProperty
    protected String path = "";
    @JsonProperty
    protected String callback = "";

    public JsonRequest(String path, JsonNode obj) {
        this.path = path;
        message = obj;
    }

    public JsonRequest() {
        this("", null);
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public JsonNode getMessage() {
        return message;
    }

    public void setMessage(JsonNode data) {
        message = data;
    }

    public String getCallback() {
        return callback;
    }

    public void setCallback(final String client_callback) {
        this.callback = client_callback;
    }

    /**
     * Get the Message this JSON event represents as the given type or null
     * if klass is null or conversion fails
     *
     * @param klass
     * @param <T>
     * @return
     */
    public <T> T as(Class<T> klass) throws JsonProcessingException {
        if (klass == null) {
            return null;
        }
        return DefaultWebSocketEventHandler.mapper.treeToValue(message, klass);
    }

    public <T> T as(String field, Class<T> klass) throws JsonProcessingException {
        if (klass == null) {
            return null;
        }
        JsonNode node = message.findValue(field);
        if (node == null) {
            return null;
        }
        return DefaultWebSocketEventHandler.mapper.treeToValue(node, klass);
    }

    public String toString() {
        return "JsonEvent{" +
                "\nTopic='" + path + '\'' +
                "\nClient callback='" + callback + '\'' +
                ",\nMessage='" + message + '\'' +
                "\n}";
    }

    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof JsonRequest)) {
            return false;
        }
        final JsonRequest request = (JsonRequest) o;
        if (callback != null ? !callback.equals(request.callback) : request.callback != null) {
            return false;
        }
        if (message != null ? !message.equals(request.message) : request.message != null) {
            return false;
        }
        if (path != null ? !path.equals(request.path) : request.path != null) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        int result = message != null ? message.hashCode() : 0;
        result = 31 * result + (path != null ? path.hashCode() : 0);
        result = 31 * result + (callback != null ? callback.hashCode() : 0);
        return result;
    }
}
