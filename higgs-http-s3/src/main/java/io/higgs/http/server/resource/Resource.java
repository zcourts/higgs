package io.higgs.http.server.resource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An optional annotation. If added to a resource class its settings are used to determine
 * how the resource is processed.
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface Resource {
    /**
     * If true a single instance of the annotated class is used to handle all requests matching the class's methods
     * If false then a new instance is created per request
     *
     * @return
     */
    boolean singleton() default false;

}
