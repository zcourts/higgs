package io.higgs.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class Attr extends HashMap<String, List<Object>> {
    public void add(String key, Object value) {
        List<Object> set = get(key);
        if (set == null) {
            set = new ArrayList<>();
            put(key, set);
        }
        set.add(value);
    }
}
