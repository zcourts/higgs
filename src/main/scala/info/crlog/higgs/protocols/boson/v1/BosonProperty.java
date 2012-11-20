package info.crlog.higgs.protocols.boson.v1;

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
@Target({ElementType.FIELD})
public @interface BosonProperty {
    /**
     * Optionally provide a name for this field.
     * If no name is provided, by default the variable name is used
     *
     * @return
     */
    public String value() default "";

    /**
     * Mark this field ignored, doing this causes the field not to be serialized
     *
     * @return
     */
    public boolean ignore() default false;
}
