package io.higgs.http.server.params;

import java.util.HashMap;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class HttpSession<K, V> extends HashMap<K, V> {
    private final HashMap<K, V> flash = new HashMap<>();

    /**
     * Adds a key value pair to the session which is good for one use.
     * Once the object is retrieved it is automatically removed
     */
    public V flash(K key, V value) {
        return flash.put(key, value);
    }

    @Override
    public V get(Object key) {
        V val = super.get(key);
        if (val == null) {
            val = flash.get(key);
            if (val != null) {
                flash.remove(key);
            }
        }
        return val;
    }

    /**
     * @return Total combination of normal session objects and flash objects
     */
    public int getSize() {
        return size() + flash.size();
    }
}
