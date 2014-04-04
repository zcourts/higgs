package io.higgs.http.server.params;

import java.util.HashMap;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class FormParams extends HashMap<String, Object> {
    public int getSize() {
        return size();
    }
}
