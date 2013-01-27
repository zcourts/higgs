package com.fillta.higgs.http.client.oauth.v1;

import org.scribe.builder.api.DefaultApi10a;
import org.scribe.model.OAuthConfig;
import org.scribe.model.OAuthConstants;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Token;
import org.scribe.oauth.OAuth10aServiceImpl;
import org.scribe.utils.MapUtils;

import java.util.Map;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class OAuth10aServiceImplHiggs extends OAuth10aServiceImpl {
    protected DefaultApi10a api;
    protected OAuthConfig config;

    public OAuth10aServiceImplHiggs(DefaultApi10a api, OAuthConfig config) {
        super(api, config);
        this.api = api;
        this.config = config;
    }

    protected void addOAuthParams(OAuthRequest request, Token token) {
        request.addOAuthParameter(OAuthConstants.TIMESTAMP, api.getTimestampService().getTimestampInSeconds());
        request.addOAuthParameter(OAuthConstants.NONCE, api.getTimestampService().getNonce());
        request.addOAuthParameter(OAuthConstants.CONSUMER_KEY, config.getApiKey());
        request.addOAuthParameter(OAuthConstants.SIGN_METHOD, api.getSignatureService().getSignatureMethod());
        request.addOAuthParameter(OAuthConstants.VERSION, getVersion());
        if (config.hasScope()) {
            request.addOAuthParameter(OAuthConstants.SCOPE, config.getScope());
        }
        request.addOAuthParameter(OAuthConstants.SIGNATURE, getSignature(request, token));

        config.log("appended additional OAuth parameters: " + MapUtils.toString(request.getOauthParameters()));
    }

    protected String getSignature(OAuthRequest request, Token token) {
        config.log("generating signature...");
        String baseString = api.getBaseStringExtractor().extract(request);
        String signature = api.getSignatureService().getSignature(baseString, config.getApiSecret(), token.getSecret());

        config.log("base string is: " + baseString);
        config.log("signature is: " + signature);
        return signature;
    }

    private void appendSignature(OAuthRequest request) {
        switch (config.getSignatureType()) {
            case Header:
                config.log("using Http Header signature");

                String oauthHeader = api.getHeaderExtractor().extract(request);
                request.addHeader(OAuthConstants.HEADER, oauthHeader);
                break;
            case QueryString:
                config.log("using Querystring signature");

                for (Map.Entry<String, String> entry : request.getOauthParameters().entrySet()) {
                    request.addQuerystringParameter(entry.getKey(), entry.getValue());
                }
                break;
        }
    }

    /**
     * @return a configured OAuthRequest
     */
    public OAuthRequest getRequestTokenHiggs() {
        config.log("obtaining request token from " + api.getRequestTokenEndpoint());
        OAuthRequest request = new OAuthRequest(api.getRequestTokenVerb(), api.getRequestTokenEndpoint());

        config.log("setting oauth_callback to " + config.getCallback());
        request.addOAuthParameter(OAuthConstants.CALLBACK, config.getCallback());
        addOAuthParams(request, OAuthConstants.EMPTY_TOKEN);
        appendSignature(request);

//        config.log("sending request...");
//        Response response = request.send();
//        String body = response.getBody();
//
//        config.log("response status code: " + response.getCode());
//        config.log("response body: " + body);
//        return api.getRequestTokenExtractor().extract(body);
        return request;
    }

    public OAuthRequest getAccessTokenHiggs(Token requestToken, String verifier) {
        config.log("obtaining access token from " + api.getAccessTokenEndpoint());
        OAuthRequest request = new OAuthRequest(api.getAccessTokenVerb(), api.getAccessTokenEndpoint());
        request.addOAuthParameter(OAuthConstants.TOKEN, requestToken.getToken());
        request.addOAuthParameter(OAuthConstants.VERIFIER, verifier);

        config.log("setting token to: " + requestToken + " and verifier to: " + verifier);
        addOAuthParams(request, requestToken);
        appendSignature(request);
        return request;
    }
}
