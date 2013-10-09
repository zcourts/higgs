package io.higgs.http.server.resource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface Consumes {
    String[] value() default { MediaType.WILDCARD };
}
