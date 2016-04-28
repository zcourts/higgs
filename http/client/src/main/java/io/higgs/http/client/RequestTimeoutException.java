package io.higgs.http.client;

import java.io.IOException;

/**
 * Created by zcourts on 28/04/2016.
 */
public class RequestTimeoutException extends IOException {
    public RequestTimeoutException(String message) {
        super(message);
    }

    public static class ConnectTimeoutException extends RequestTimeoutException {
        public ConnectTimeoutException(String message) {
            super(message);
        }
    }

    public static class ResponseTimeoutException extends RequestTimeoutException {
        public ResponseTimeoutException(String message) {
            super(message);
        }
    }
}
