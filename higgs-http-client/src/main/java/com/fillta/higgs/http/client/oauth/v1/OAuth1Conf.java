package com.fillta.higgs.http.client.oauth.v1;

import org.scribe.builder.api.DefaultApi10a;
import org.scribe.model.OAuthConstants;
import org.scribe.model.OAuthRequest;
import org.scribe.model.SignatureType;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuth10aServiceImpl;

import java.util.Map;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class OAuth1Conf {
    protected String apiKey;
    protected String apiSecret;
    protected DefaultApi10a api;
    protected String callback;
    protected String scope;
    protected static final String VERSION = "1.0";

    public OAuth1Conf(DefaultApi10a api, String key, String secret, String url, String scope) {
        this.apiKey = key;
        this.apiSecret = secret;
        this.callback = url != null ? url : OAuthConstants.OUT_OF_BAND;
        this.api = api;
        this.scope = scope != null ? scope : null;
    }

    /**
     * Takes essential parts of {@link OAuth10aServiceImpl} and generates the request
     * and sign it
     */
    public OAuthRequest createRequest(Verb verb, String url) {
        return new OAuthRequest(verb, url);
    }

    /**
     * Generate the request's parameters and signature using an empty string for the token secret.
     * Useful when creating request tokens which obviously fo not have an access token secret
     */
    public OAuth1Conf addOAuthParams(OAuthRequest request) {
        return addOAuthParams(request, OAuthConstants.EMPTY_TOKEN.getSecret());
    }

    /**
     * Generate the request's parameters and signature using the given tokenSecret.
     * Useful when signing a request and the access token secret needs to be provided.
     */

    public OAuth1Conf addOAuthParams(OAuthRequest request, String tokenSecret) {
        request.addOAuthParameter(OAuthConstants.TIMESTAMP, api.getTimestampService().getTimestampInSeconds());
        request.addOAuthParameter(OAuthConstants.NONCE, api.getTimestampService().getNonce());
        request.addOAuthParameter(OAuthConstants.CONSUMER_KEY, apiKey);
        request.addOAuthParameter(OAuthConstants.SIGN_METHOD, api.getSignatureService().getSignatureMethod());
        request.addOAuthParameter(OAuthConstants.VERSION, VERSION);
        if (scope != null) {
            request.addOAuthParameter(OAuthConstants.SCOPE, scope);
        }
        String baseString = api.getBaseStringExtractor().extract(request);
        String signature = api.getSignatureService().getSignature(baseString, apiSecret, tokenSecret);
        System.out.println(baseString);
        request.addOAuthParameter(OAuthConstants.SIGNATURE, signature);
        return this;
    }

    public void appendSignature(SignatureType signature, OAuthRequest request) {
        switch (signature) {
            case Header:
                String oauthHeader = api.getHeaderExtractor().extract(request);
                request.addHeader(OAuthConstants.HEADER, oauthHeader);
                break;
            case QueryString:
                for (Map.Entry<String, String> entry : request.getOauthParameters().entrySet()) {
                    request.addQuerystringParameter(entry.getKey(), entry.getValue());
                }
                break;
        }
    }

    public DefaultApi10a api() {
        return api;
    }

    public String scope() {
        return scope;
    }

    public String callback() {
        return callback;
    }
}
