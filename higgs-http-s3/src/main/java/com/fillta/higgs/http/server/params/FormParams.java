package com.fillta.higgs.http.server.params;

import java.util.HashMap;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class FormParams extends HashMap<String, String> {
    public int getSize() {
        return size();
    }
}
