package com.fillta.higgs.http.server.params;

import java.util.HashMap;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class FormFiles extends HashMap<String, HttpFile> {
	public int getSize() {
		return size();
	}
}
