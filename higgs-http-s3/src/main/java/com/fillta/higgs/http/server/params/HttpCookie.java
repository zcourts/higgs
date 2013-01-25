package com.fillta.higgs.http.server.params;

import com.fillta.higgs.reflect.ReflectionUtil;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.DefaultCookie;
import io.netty.handler.codec.http.DefaultHttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class HttpCookie extends DefaultCookie {
    private ReflectionUtil reflection = new ReflectionUtil();
    protected Logger log = LoggerFactory.getLogger(getClass());

    public HttpCookie(String name, String value) {
        super(name, value);
        setPath("/");
    }

    public HttpCookie(Cookie cookie) {
        this(cookie.getName(), cookie.getValue());
        List<Field> fields = reflection.getAllFields(new ArrayList<Field>(), DefaultHttpRequest.class, 10);
        for (Field field : fields) {
            try {
                field.setAccessible(true);
                field.set(this, field.get(cookie));
            } catch (Throwable t) {
                log.warn("Error copying cookie field", t);
            }
        }
    }
}
