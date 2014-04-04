package io.higgs.http.server.params;

import io.higgs.core.reflect.ReflectionUtil;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.DefaultCookie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class HttpCookie extends DefaultCookie {
    protected Logger log = LoggerFactory.getLogger(getClass());

    public HttpCookie(String name, String value) {
        super(name, value);
        setPath("/");
    }

    public HttpCookie(Cookie cookie) {
        this(cookie.getName(), cookie.getValue());
        Set<Field> fields = ReflectionUtil.getAllFields(new HashSet<Field>(), DefaultCookie.class, 1);
        for (Field field : fields) {
            try {
                if (!Modifier.isFinal(field.getModifiers())) {
                    field.setAccessible(true);
                    field.set(this, field.get(cookie));
                }
            } catch (Throwable t) {
                log.warn("Error copying cookie field", t);
            }
        }
    }
}
