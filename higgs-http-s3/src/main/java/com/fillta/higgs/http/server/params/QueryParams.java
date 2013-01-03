package com.fillta.higgs.http.server.params;

import java.util.HashMap;
import java.util.List;

/**
 * Represents the set of query string parameters available with a request
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class QueryParams extends HashMap<String, List<String>> {
	/**
	 * Gets the first value of the query string parameter with the given name
	 *
	 * @param name the name of the query string parameter to get
	 * @return the value of the parameter or null if it doesn't exist
	 */
	public String getFirst(String name) {
		List<String> vals = get(name);
		if (vals == null)
			return null;
		return vals.get(0);
	}
}
