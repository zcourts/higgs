package io.higgs.ws.client;

import java.io.IOException;

import static io.higgs.ws.client.WebSocketClient.MAPPER;

/**
 * @author Courtney Robinson <courtney.robinson@datasift.com>
 */
public class WebSocketMessage {
    protected final String data;

    public WebSocketMessage(String text) {
        this.data = text;
    }

    public String data() {
        return data;
    }

    /**
     * Convert this web socket object to an instance of the given class
     *
     * @param klass the class to create an instance of
     * @return an instance of the given class created from the data or null if an erro occurred
     */
    public <T> T as(Class<T> klass) {
        try {
            return MAPPER.readValue(data, klass);
        } catch (IOException e) {
            return null;
        }
    }

    public String toString() {
        return data;
    }
}
