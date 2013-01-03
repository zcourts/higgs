package com.fillta.higgs.http.server.params;

import java.util.HashMap;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class HttpSession<K, V> extends HashMap<K, V> {
	public int getSize() {
		return size();
	}
}
