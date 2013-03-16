package io.higgs.ws;

import com.fasterxml.jackson.databind.JsonNode;
import io.higgs.method;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public interface JsonRequestEvent {
    /**
     * Un-marshall a JsonEvent's message into the given type returning null on failure or if klass is null
     *
     * @param klass
     * @param <T>
     * @return
     */
    <T> T as(Class<T> klass);

    /**
     * Get the name of the client callback
     *
     * @return
     */
    String getCallback();

    /**
     * The callback function that will receive responses...if any
     *
     * @param callback
     */
    void setCallback(String callback);

    /**
     * Set the name of the server callback to be invoked
     *
     * @param topic
     */
    void setTopic(String topic);

    /**
     * @return The name registered with {@link method} or a plain string.
     */
    String getTopic();

    JsonNode getMessage();

    void setMessage(JsonNode o);
}
