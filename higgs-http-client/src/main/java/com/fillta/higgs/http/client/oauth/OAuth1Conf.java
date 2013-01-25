package com.fillta.higgs.http.client.oauth;

import com.google.common.base.Optional;
import org.scribe.builder.api.DefaultApi10a;
import org.scribe.model.OAuthConstants;
import org.scribe.model.OAuthRequest;
import org.scribe.oauth.OAuth10aServiceImpl;

import java.io.OutputStream;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class OAuth1Conf {
    protected String apiKey;
    protected String apiSecret;
    protected DefaultApi10a api;
    protected String callback;
    protected String scope;
    protected OAuthRequest request;
    protected static final String VERSION = "1.0";

    public OAuth1Conf(DefaultApi10a api, String key, String secret, Optional<String> url,
                      Optional<String> scope, Optional<OutputStream> stream) {
        this.apiKey = key;
        this.apiSecret = secret;
        this.callback = url.isPresent() ? url.get() : OAuthConstants.OUT_OF_BAND;
        this.api = api;
        this.scope = scope.isPresent() ? scope.get() : null;
        request = new OAuthRequest(api.getRequestTokenVerb(), api.getRequestTokenEndpoint());
        createRequest();
    }

    /**
     * Takes essential parts of {@link OAuth10aServiceImpl} and generates the request
     */
    protected void createRequest() {
        request.addOAuthParameter(OAuthConstants.CALLBACK, callback);
        request.addOAuthParameter(OAuthConstants.TIMESTAMP, api.getTimestampService().getTimestampInSeconds());
        request.addOAuthParameter(OAuthConstants.NONCE, api.getTimestampService().getNonce());
        request.addOAuthParameter(OAuthConstants.CONSUMER_KEY, apiKey);
        request.addOAuthParameter(OAuthConstants.SIGN_METHOD, api.getSignatureService().getSignatureMethod());
        request.addOAuthParameter(OAuthConstants.VERSION, VERSION);
        if (scope != null) {
            request.addOAuthParameter(OAuthConstants.SCOPE, scope);
        }
        String baseString = api.getBaseStringExtractor().extract(request);
        String signature = api.getSignatureService().getSignature(baseString, apiSecret,
                OAuthConstants.EMPTY_TOKEN.getSecret());
        request.addOAuthParameter(OAuthConstants.SIGNATURE, signature);
        //
        String oauthHeader = api.getHeaderExtractor().extract(request);
        request.addHeader(OAuthConstants.HEADER, oauthHeader);
    }

    public OAuthRequest request() {
        return request;
    }
}
