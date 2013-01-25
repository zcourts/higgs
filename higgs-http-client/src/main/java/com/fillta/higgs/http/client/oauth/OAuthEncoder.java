package com.fillta.higgs.http.client.oauth;

import com.fillta.higgs.http.client.oauth.OAuthException;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * see original https://github.com/fernandezpablo85/scribe-java/blob/master/src/main/java/org/scribe/utils/OAuthEncoder.java
 *
 * @author: Pablo Fernandez
 */
public class OAuthEncoder {
	private static String CHARSET = "UTF-8";
	private static final Map<String, String> ENCODING_RULES;

	static {
		Map<String, String> rules = new HashMap<String, String>();
		rules.put("*", "%2A");
		rules.put("+", "%20");
		rules.put("%7E", "~");
		ENCODING_RULES = Collections.unmodifiableMap(rules);
	}

	public static String encode(String plain) {
		String encoded = "";
		try {
			encoded = URLEncoder.encode(plain, CHARSET);
		} catch (UnsupportedEncodingException uee) {
			throw new OAuthException("Charset not found while encoding string: " + CHARSET, uee);
		}
		for (Map.Entry<String, String> rule : ENCODING_RULES.entrySet()) {
			encoded = applyRule(encoded, rule.getKey(), rule.getValue());
		}
		return encoded;
	}

	private static String applyRule(String encoded, String toReplace, String replacement) {
		return encoded.replaceAll(Pattern.quote(toReplace), replacement);
	}

	public static String decode(String encoded) {
		try {
			return URLDecoder.decode(encoded, CHARSET);
		} catch (UnsupportedEncodingException uee) {
			throw new OAuthException("Charset not found while decoding string: " + CHARSET, uee);
		}
	}
}