package com.fillta.higgs.http.client.oauth;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class OAuth1Header {
	private String name = "Authorization";
	private String componentPrefix = "OAuth";
	private SortedMap<String, Object> components = new TreeMap<>();

	/**
	 * Create an OAuth request with header "Authorization"
	 */
	public OAuth1Header() {
	}

	public OAuth1Header(final String name) {
		this.name = name;
	}

	public OAuth1Header(final String name, final SortedMap<String, Object> components) {
		this.name = name;
		this.components = components;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public String getComponentPrefix() {
		return componentPrefix;
	}

	/**
	 * @param componentPrefix Component prefix is the string prepended to the encoded Authorization value e.g.
	 *                        OAuth oauth_nonce=xyz,oauth_signature=x123y  where "OAuth" is the prefix
	 */
	public void setComponentPrefix(final String componentPrefix) {
		this.componentPrefix = componentPrefix;
	}

	public Map<String, Object> getComponents() {
		return components;
	}

	public void setComponents(final SortedMap<String, Object> components) {
		this.components = components;
	}

	public void setComponent(String name, Object value) {
		components.put(name, value);
	}

	public OAuth1Header setComponent(String name, String value) {
		components.put(name, value);
		return this;
	}

	/**
	 * Sets a URL component. The URL is URL-encoded
	 *
	 * @param name the component name
	 * @param url  the URL to associate with the given name
	 * @return true on success, false if {@link UnsupportedEncodingException} is thrown
	 */
	public boolean setURLComponent(String name, String url) {
		try {
			components.put(name, URLEncoder.encode(url, "UTF-8"));
			return true;
		} catch (UnsupportedEncodingException e) {
			return false;
		}
	}

	public OAuth1Header oauth_callback(String oauth_callback) {
		setComponent("oauth_callback", oauth_callback);
		return this;
	}

	public OAuth1Header oauth_consumer_key(String oauth_consumer_key) {
		setComponent("oauth_consumer_key", oauth_consumer_key);
		return this;
	}

	public OAuth1Header oauth_nonce(String oauth_nonce) {
		setComponent("oauth_nonce", oauth_nonce);
		return this;
	}

	public OAuth1Header oauth_signature(String oauth_signature) {
		setComponent("oauth_signature", oauth_signature);
		return this;
	}

	public OAuth1Header oauth_signature_method(String oauth_signature_method) {
		setComponent("oauth_signature_method", oauth_signature_method);
		return this;
	}

	public OAuth1Header oauth_timestamp(long oauth_timestamp) {
		setComponent("oauth_timestamp", oauth_timestamp);
		return this;
	}

	public OAuth1Header oauth_version(String oauth_version) {
		setComponent("oauth_version", oauth_version);
		return this;
	}

	public OAuth1Header twitter() {
		oauth_signature_method("HMAC-SHA1");
		oauth_timestamp(System.currentTimeMillis() / 1000);
		oauth_version("1.0");
		return this;
	}
}
