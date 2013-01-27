package com.fillta.higgs.http.client.oauth.v1;

import com.fillta.higgs.http.client.HttpRequestBuilder;
import org.scribe.model.SignatureType;

import static org.scribe.model.SignatureType.Header;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class OAuth1RequestBuilderOld {
    protected OAuth1Conf authService;
    protected HttpRequestBuilder requestBuilder;
    protected SignatureType signature = Header;
//
//    public OAuth1RequestBuilderOld(HttpRequestBuilder requestBuilder) {
//        this.requestBuilder = requestBuilder;
//    }
//
//    public <A extends DefaultApi10a> OAuth1RequestBuilderOld config(Class<A> api, String apiKey, String secret) {
//        return config(api, apiKey, secret, null);
//    }
//
//    public <A extends DefaultApi10a> OAuth1RequestBuilderOld config(Class<A> api, String apiKey, String secret,
//                                                                 String callbackURL) {
//        return config(api, apiKey, secret, callbackURL, null);
//    }
//
//    public <A extends DefaultApi10a> OAuth1RequestBuilderOld config(Class<A> api, String apiKey, String secret,
//                                                                 String callbackURL,
//                                                                 String scope) {
//        try {
//            authService = new OAuth1Conf(api.newInstance(), apiKey, secret, callbackURL, scope);
//        } catch (Throwable e) {
//            throw new OAuthException("Unable to create OAuth1 API instance", e);
//        }
//        return this;
//    }
//
//    public OAuth1RequestBuilderOld signature(SignatureType type) {
//        signature = type;
//        return this;
//    }
//
//    public OAuthRequest getRequest(Verb verb, String url) {
//        if (authService == null) {
//            throw new OAuthException("OAuth 1 service is not configured. Call one of the oAuth methods first", null);
//        }
//        OAuthRequest request = authService.createRequest(verb, url);
//        return request;
//    }
//
//    public void configureHiggsRequest(OAuthRequest request) {
//        switch (signature) {
//            case Header:
//                for (String name : request.getHeaders().keySet()) {
//                    String value = request.getHeaders().get(name);
//                    requestBuilder.header(name, value);
//                }
//                break;
//            case QueryString:
//                //remove any previously added Authorization header - query string is already added to URL
//                requestBuilder.getRequestHeaders().remove(OAuthConstants.HEADER);
//        }
//        requestBuilder.url(request.getCompleteUrl());
//    }
//
//    protected void verbFromScribeToHiggs(Verb verb) {
//        switch (verb) {
//            case POST:
//                requestBuilder.POST();
//                break;
//            case GET:
//                requestBuilder.GET();
//                break;
//            case DELETE:
//                requestBuilder.DELETE();
//                break;
//            case PUT:
//                requestBuilder.PUT();
//                break;
//            default:
//                throw new UnsupportedOperationException(String.format("Unsupported HTTP Verb : %s", verb));
//        }
//    }
//
//    protected Verb verbFromHiggsToScribe(HttpMethod method) {
//        if (HttpMethod.POST.name().equalsIgnoreCase(method.name())) {
//            return Verb.POST;
//        }
//        if (HttpMethod.PUT.name().equalsIgnoreCase(method.name())) {
//            return Verb.PUT;
//        }
//        if (HttpMethod.GET.name().equalsIgnoreCase(method.name())) {
//            return Verb.GET;
//        }
//        if (HttpMethod.DELETE.name().equalsIgnoreCase(method.name())) {
//            return Verb.DELETE;
//        }
//        throw new UnsupportedOperationException(String.format("Unsupported HTTP Verb : %s", method.name()));
//    }
//
//    protected void checkStatus(HTTPResponse a) {
//        if (a.status() < 200 || a.status() >= 300) {
//            throw new OAuthResponseException("Invalid oAuth response", a);
//        }
//    }
//
//    /**
//     * Make an OAuth request to the configured API which will return a request token for use in authorization
//     * later.
//     *
//     * @param callback a callback that will be notified when the response is available
//     * @return this builder
//     */
//    public OAuth1RequestBuilderOld requestToken(final Function1<OAuth1RequestToken> callback) {
//        String url = authService.api().getRequestTokenEndpoint();
//        OAuthRequest request = getRequest(authService.api().getRequestTokenVerb(), url);
//        request.addOAuthParameter(OAuthConstants.CALLBACK, authService.callback());
//        authService.addOAuthParams(request);
//        authService.appendSignature(signature, request);
//        configureHiggsRequest(request);
//        verbFromScribeToHiggs(authService.api().getRequestTokenVerb());
//        requestBuilder.build(new Function1<HTTPResponse>() {
//            public void apply(final HTTPResponse a) {
//                checkStatus(a);
//                a.readAll(new Function1<String>() {
//                    public void apply(String a) {
//                        if (a != null) {
//                            callback.apply(new OAuth1RequestToken(OAuth1RequestBuilderOld.this, a));
//                        }
//                    }
//                });
//            }
//        });
//        return this;
//    }
//
//    /**
//     * Trade in a request token for an access token which can be used to make requests on behalf of a given user.
//     *
//     * @param token    the request token to trade in
//     * @param verifier the verifier value the service provided after the user authorized your app
//     * @param callback invoked when an access token is successfully retrieved
//     */
//    public void accessToken(OAuth1RequestToken token, String verifier, final Function1<OAuth1AccessToken> callback) {
//        String url = authService.api().getAccessTokenEndpoint();
//        OAuthRequest request = getRequest(authService.api().getAccessTokenVerb(), url);
//
//        request.addOAuthParameter(OAuthConstants.TOKEN, token.getRequestToken());
//        request.addOAuthParameter(OAuthConstants.VERIFIER, verifier);
//
//        authService.addOAuthParams(request);
//        authService.appendSignature(signature, request);
//
//        configureHiggsRequest(request);
//        verbFromScribeToHiggs(authService.api().getAccessTokenVerb());
//
//        requestBuilder.build(new Function1<HTTPResponse>() {
//            public void apply(HTTPResponse response) {
//                response.readAll(new Function1<String>() {
//                    public void apply(String a) {
//                        callback.apply(new OAuth1AccessToken(OAuth1RequestBuilderOld.this, a));
//                    }
//                });
//            }
//        });
//    }
//
//    public HttpRequestBuilder signRequest(OAuth1AccessToken token, Function1<HTTPResponse> callback) {
//        return signRequest(token.oAuthToken(), token.oAuthTokenSecret(), callback);
//    }
//
//    /**
//     * Sign a request to the URL configured in the {@link com.fillta.higgs.http.client.HttpRequestBuilder}.
//     * You MUST set the URL AND HTTP METHOD before signing a request since both of these will be used
//     * in the signing process. Failure to do will create an invalid OAuth signature.
//     *
//     * @param token       the access token to sign the request for...
//     * @param tokenSecret the token secret for the given access token
//     */
//    public HttpRequestBuilder signRequest(String token, String tokenSecret, Function1<HTTPResponse> callback) {
//        if (token == null) {
//            throw new IllegalArgumentException("Access token cannot be null");
//        }
//        OAuthRequest request = getRequest(verbFromHiggsToScribe(requestBuilder.getRequestMethod()),
//                requestBuilder.url().toExternalForm());
//        Map<String, List<String>> params = new QueryStringDecoder(requestBuilder.url().toExternalForm()).parameters();
//        for (String name : params.keySet()) {
//            List<String> values = params.get(name);
//            String value = values == null || values.size() == 0 ? "" : values.get(0);
//            request.addQuerystringParameter(name, value);
//        }
//        //http://hueniverse.com/oauth/guide/authentication/
//        //only parameters included in a single-part 'application/x-www-form-urlencoded' are used in signing
//        //if it is a post or put request.
//        HttpMethod method = requestBuilder.getRequestMethod();
//        if (method.equals(HttpMethod.POST) || method.equals(HttpMethod.PUT)) {
//            Map<String, Object> form = requestBuilder.getFormParameters();
//            for (String name : form.keySet()) {
//                Object v = form.get(name);
//                String value = v == null ? "" : v.toString();
//                request.addBodyParameter(name, value);
//            }
//        }
//        OAuth10aServiceImpl oauth1 = new OAuth10aServiceImpl(authService.api(),
//                new OAuthConfig(
//                        authService.apiKey,
//                        authService.apiSecret,
//                        authService.callback,
//                        signature,
//                        authService.scope,
//                        null
//                ));
//        oauth1.signRequest(new Token(token, tokenSecret), request);
//        System.out.println(request.send().getBody());
////        if (!token.isEmpty()) {
////            request.addOAuthParameter(OAuthConstants.TOKEN, token);
////            authService.addOAuthParams(request, tokenSecret);
////        } else {
////            authService.addOAuthParams(request);
////        }
////        authService.appendSignature(signature, request);
//        configureHiggsRequest(request);
//        requestBuilder.build(callback);
//        return requestBuilder;
//    }
}
