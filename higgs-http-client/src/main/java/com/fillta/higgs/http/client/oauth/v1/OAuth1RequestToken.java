package com.fillta.higgs.http.client.oauth.v1;

import io.netty.handler.codec.http.QueryStringDecoder;
import org.scribe.model.Token;

import java.util.List;
import java.util.Map;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class OAuth1RequestToken {
    protected String oauthToken;
    protected String oauthTokenSecret;
    protected boolean oauthCallbackConfirmed;
    protected OAuth1RequestBuilder requestBuilder;

    public OAuth1RequestToken(OAuth1RequestBuilder oAuth1RequestBuilder, String response) {
        requestBuilder = oAuth1RequestBuilder;
        Map<String, List<String>> params = new QueryStringDecoder("?" + response).parameters();
        oauthToken = params.get("oauth_token").get(0);
        oauthTokenSecret = params.get("oauth_token_secret").get(0);
        oauthCallbackConfirmed = Boolean.parseBoolean(params.get("oauth_callback_confirmed").get(0));
    }

    /**
     * @return The URL users should be redirected to in order to authorize the request
     */
    public String authorizationUrl() {
        return requestBuilder.authService.api.getAuthorizationUrl(new Token(oauthToken, oauthTokenSecret));
    }

    public OAuth1RequestBuilder getRequestBuilder() {
        return requestBuilder;
    }

    public String getRequestToken() {
        return oauthToken;
    }

    public String getOauthTokenSecret() {
        return oauthTokenSecret;
    }

    public boolean isOauthCallbackConfirmed() {
        return oauthCallbackConfirmed;
    }

    public String toString() {
        return "OAuth1RequestToken{" +
                "oauthToken='" + oauthToken + '\'' +
                ", oauthTokenSecret='" + oauthTokenSecret + '\'' +
                ", oauthCallbackConfirmed=" + oauthCallbackConfirmed +
                '}';
    }
}
