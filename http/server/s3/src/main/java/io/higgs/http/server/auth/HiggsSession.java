package io.higgs.http.server.auth;

import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.SimpleSession;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class HiggsSession extends SimpleSession implements Map<Object, Object> {
    /**
     * Intended for MsgPack's use only, use {@link #HiggsSession(java.io.Serializable)} instead.
     */
    public HiggsSession() {
        //MsgPack
    }

    public HiggsSession(Serializable id) {
        setId(id);
    }

    /**
     * Copy constructor. Used for serialization to convert to HiggsSession
     *
     * @param that the session to copy from
     */
    public HiggsSession(Session that) {
        for (Object o : that.getAttributeKeys()) {
            setAttribute(o, that.getAttribute(o));
        }
    }

    @Override
    public Map<Object, Object> getAttributes() {
        if (super.getAttributes() == null) {
            setAttributes(new HashMap<>());
        }
        return super.getAttributes();
    }

    /**
     * Adds a key value pair to the session which is good for one use.
     * Once the object is retrieved it is automatically removed
     */
    public void flash(Object key, Object value) {
        setAttribute(key, value instanceof FlashValue ? value : new FlashValue(value));
    }

    @Override
    public Object getAttribute(Object key) {
        Object value = super.getAttribute(key);
        if (value instanceof FlashValue) {
            removeAttribute(key);
            return ((FlashValue) value).getValue();
        }
        return value;
    }

    @Override
    public int size() {
        return getAttributes().size();
    }

    @Override
    public boolean isEmpty() {
        return getAttributes().isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return getAttributes().containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return getAttributes().containsValue(value);
    }

    @Override
    public Object get(Object key) {
        return getAttributes().get(key);
    }

    @Override
    public Object put(Object key, Object value) {
        return getAttributes().put(key, value);
    }

    @Override
    public Object remove(Object key) {
        return getAttributes().remove(key);
    }

    @Override
    public void putAll(Map<?, ?> m) {
        getAttributes().putAll(m);
    }

    @Override
    public void clear() {
        getAttributes().clear();
    }

    @Override
    public Set<Object> keySet() {
        return getAttributes().keySet();
    }

    @Override
    public Collection<Object> values() {
        return getAttributes().values();
    }

    @Override
    public Set<Entry<Object, Object>> entrySet() {
        return getAttributes().entrySet();
    }
}
