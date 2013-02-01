package com.fillta.higgs.http.client.oauth.v1;

import io.netty.handler.codec.http.QueryStringDecoder;
import org.scribe.model.Token;

import java.util.List;
import java.util.Map;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class OAuth1AccessToken {
    protected OAuth1RequestBuilder requestBuilder;
    protected Map<String, List<String>> params;

    public OAuth1AccessToken(OAuth1RequestBuilder oAuth1RequestBuilder, String response) {
        requestBuilder = oAuth1RequestBuilder;
        params = new QueryStringDecoder("?" + response).parameters();
    }

    public OAuth1AccessToken(OAuth1RequestBuilder builder, Token token) {
        this(builder, token.getRawResponse());
    }

    /**
     * @return The access token value
     */
    public String oAuthToken() {
        return param("oauth_token");
    }

    /**
     * @return the token secret
     */
    public String oAuthTokenSecret() {
        return param("oauth_token");
    }

    /**
     * @return true if both {@link #oAuthToken()} and {@link #oAuthTokenSecret()} are null
     */
    public boolean isEmpty() {
        return oAuthToken() == null && oAuthTokenSecret() == null;
    }

    /**
     * Get a single param out of the data that was returned in response to an access token request.
     * Some services such as twitter return "user_id", "screen_name" and/or other parameters, use this method to
     * access them.
     *
     * @param name the name of the param to get
     * @return the value of that given param or null
     */
    public String param(String name) {
        List<String> str = params.get(name);
        return str == null || str.size() == 0 ? null : str.get(0);
    }

    public String toString() {
        return "OAuth1AccessToken{" +
                "\n oauth_token=" + oAuthToken() +
                "\n oauth_token_secret=" + oAuthTokenSecret() +
                "\n params=" + params +
                '}';
    }
}
