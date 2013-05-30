package io.higgs.http.server;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class WrappedResponse {
    private final String callback;
    private final Object data;

    public WrappedResponse(Object data) {
        this(null, data);
    }

    public WrappedResponse(String callback, Object data) {
        this.callback = callback;
        this.data = data;
    }

    public String callback() {
        return callback;
    }

    public Object data() {
        return data;
    }

    @Override
    public String toString() {
        return "WrappedResponse{" +
                "callback='" + callback + '\'' +
                ", data=" + data +
                '}';
    }
}
