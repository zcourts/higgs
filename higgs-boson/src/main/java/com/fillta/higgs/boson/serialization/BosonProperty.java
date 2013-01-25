package com.fillta.higgs.boson.serialization;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Explicitly register an object's field to be serialized or ignored
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.TYPE, ElementType.METHOD })
public @interface BosonProperty {
    /**
     * Optionally provide a name for this field.
     * If no name is provided, by default the variable name is used
     *
     * @return
     */
    String value() default "";

    /**
     * Mark this field ignored, doing this causes the field not to be serialized
     *
     * @return
     */
    boolean ignore() default false;

    boolean ignoreInheritedFields() default false;
}
