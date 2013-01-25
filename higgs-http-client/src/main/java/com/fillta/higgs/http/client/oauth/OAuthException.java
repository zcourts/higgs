package com.fillta.higgs.http.client.oauth;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class OAuthException extends RuntimeException {
    public OAuthException(final String msg, final Throwable cause) {
        super(msg, cause);
    }
}
