package io.higgs.ws;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * A simple web socket event with a topic and message property.
 * topic is the method to be invoked on either the client or server
 * message is a JSON string object
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class JsonRequest implements JsonRequestEvent {
    @JsonIgnore
    protected final Logger log = LoggerFactory.getLogger(getClass());
    @JsonIgnore
    protected String rawString;
    @JsonProperty
    protected JsonNode message;
    @JsonProperty
    protected String topic = "";
    @JsonProperty
    protected String callback = "";

    public JsonRequest(String topic, JsonNode obj) {
        this.topic = topic;
        message = obj;
    }

    public JsonRequest() {
        this("", null);
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
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
    public <T> T as(Class<T> klass) {
        if (klass == null) {
            return null;
        }
        try {
            return WebSocketServer.mapper.treeToValue(message, klass);
//            return WebSocketServer.mapper.reader(klass).readValue(message.traverse());
        } catch (IOException e) {
            log.warn(String.format("Unable to decode message to type. Type : %s\n Message string : %s",
                    klass.getName(), message.toString()), e);
            return null;
        }
    }

    public String toString() {
        return "JsonEvent{" +
                "\nTopic='" + topic + '\'' +
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
        if (topic != null ? !topic.equals(request.topic) : request.topic != null) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        int result = message != null ? message.hashCode() : 0;
        result = 31 * result + (topic != null ? topic.hashCode() : 0);
        result = 31 * result + (callback != null ? callback.hashCode() : 0);
        return result;
    }
}
