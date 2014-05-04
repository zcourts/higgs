package io.higgs.http.server.auth;

import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.SimpleSession;

import java.io.Serializable;
import java.util.Collection;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class HiggsSession extends SimpleSession {
    public HiggsSession(Serializable id) {
        setId(id);
    }

    /**
     * Adds a key value pair to the session which is good for one use.
     * Once the object is retrieved it is automatically removed
     */
    public void flash(Object key, Object value) {
        setAttribute(key, new FlashValue(value));
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

    public void fromSession(Session session) {
        if (session instanceof SimpleSession) {
            setAttributes(((SimpleSession) session).getAttributes());
        } else {
            Collection<Object> attrs = session.getAttributeKeys();
            for (Object key : attrs) {
                setAttribute(key, session.getAttribute(key));
            }
        }
    }
}
