package com.fillta.higgs.ws;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class WebSocketException extends RuntimeException {
    public WebSocketException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
