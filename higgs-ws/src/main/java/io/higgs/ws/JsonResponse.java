package io.higgs.ws;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class JsonResponse implements JsonResponseEvent {
    @JsonProperty
    private String callback = "";
    @JsonProperty
    private JsonNode message;

    /**
     * Creates a JSON response to be sent back in reply to the given request
     *
     * @param request  the request object received from the client. It'll have the client callback
     * @param response the response object to serialize and send to the client
     */
    public JsonResponse(JsonRequestEvent request, Object response) {
        this(request.getCallback(), response);
    }

    /**
     * Creates a JSON response to be sent back in reply to the given request
     *
     * @param callback the id of the callback function that will be executed on the client when this
     *                 response is received
     * @param response the response object to serialize and send to the client
     */
    public JsonResponse(String callback, Object response) {
        this.callback = callback;
        message = WebSocketServer.mapper.createObjectNode().POJONode(response);
    }

    public String getCallback() {
        return callback;
    }

    public void setCallback(final String callback) {
        this.callback = callback;
    }

    public Object getMessage() {
        return message;
    }

    public void setMessage(Object o) {
        if (o instanceof JsonNode) {
            setMessage((JsonNode) o);
        } else {
            setMessage(WebSocketServer.mapper.createObjectNode().POJONode(o));
        }
    }

    public void setMessage(JsonNode data) {
        message = data;
    }

    public String toString() {
        return "JsonResponse{" +
                "callback='" + callback + '\'' +
                ", message=" + message +
                '}';
    }

    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof JsonResponse)) {
            return false;
        }

        final JsonResponse response = (JsonResponse) o;

        if (callback != null ? !callback.equals(response.callback) : response.callback != null) {
            return false;
        }
        if (message != null ? !message.equals(response.message) : response.message != null) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        int result = callback != null ? callback.hashCode() : 0;
        result = 31 * result + (message != null ? message.hashCode() : 0);
        return result;
    }
}
