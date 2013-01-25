package com.fillta.higgs.http.client.oauth;

import org.scribe.builder.api.Api;
import org.scribe.model.SignatureType;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class OAuthConf {
	protected String apiKey;
	protected String apiSecret;
	protected Api api;
	protected String callback;
	protected String scope;
	protected SignatureType signatureType;

	public OAuthConf(String apiKey, String apiSecret, String callback, Api api, String scope) {
		this.apiKey = apiKey;
		this.apiSecret = apiSecret;
		this.callback = callback;
		this.api = api;
		this.scope = scope;
		this.signatureType = SignatureType.Header;
	}
}
