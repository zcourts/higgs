package io.higgs.http.client.oauth;

import io.higgs.http.client.HTTPResponse;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class OAuthResponseException extends RuntimeException {
    private HTTPResponse response;

    public OAuthResponseException(String msg, HTTPResponse response) {
        super(msg);
        this.response = response;
    }

    /**
     * @return The response which resulted in this exception being thrown
     */
    public HTTPResponse response() {
        return response;
    }

    public String toString() {
        return "OAuthResponseException{" +
                "response=\n" + response +
                '}';
    }
}
