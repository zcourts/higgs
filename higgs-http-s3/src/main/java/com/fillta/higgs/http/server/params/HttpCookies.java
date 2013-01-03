package com.fillta.higgs.http.server.params;

import java.util.HashMap;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class HttpCookies extends HashMap<String, HttpCookie> {
	public String getValue(String name) {
		HttpCookie cookie = get((Object) name);
		if (cookie != null) {
			return cookie.getValue();
		}
		return null;
	}
}
